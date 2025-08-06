package com.wildrew.jobstat.community.comment.usecase.command

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.comment.repository.FakeCommentRepository
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.service.CommentServiceImpl
import com.wildrew.jobstat.community.comment.utils.CommentConstants
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.community.utils.FakePasswordUtil
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*
import org.springframework.data.domain.Pageable

@DisplayName("CreateComment UseCase 테스트")
class CreateCommentUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var passwordUtil: FakePasswordUtil

    private lateinit var commentService: CommentService

    private lateinit var theadContextUtils: TheadContextUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher

    private lateinit var createComment: CreateComment

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private val testUserId = 1L
    private val guestUserId: Long? = null

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        passwordUtil = FakePasswordUtil()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        theadContextUtils = mock()
        eventPublisher = mock()

        createComment =
            CreateComment(
                commentService = commentService,
                theadContextUtils = theadContextUtils,
                passwordUtil = passwordUtil,
                communityCommandEventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        assertEquals(0, boardRepository.findById(testBoard.id).commentCount)
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
        commentRepository.clear()
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("로그인 사용자가 유효한 정보로 댓글 생성 성공")
        fun `given logged in user and valid request, when execute, then create comment and publish event`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(testUserId)
            val requestDto =
                CreateComment.Request(
                    content = "로그인 사용자 댓글 내용",
                    author = "멤버",
                    password = null, // 로그인 시 불필요
                )
            val request = requestDto.of(testBoard.id)

            // When
            val response = createComment(request)

            // Then
            assertNotNull(response.id)
            assertEquals(request.content, response.content)
            assertEquals(request.author, response.author)
            assertEquals(testBoard.id.toString(), response.boardId)
            assertNotNull(response.createdAt)

            // DB 상태 확인
            val createdComment = commentRepository.findById(response.id.toLong())
            assertEquals(request.content, createdComment.content)
            assertEquals(request.author, createdComment.author)
            assertEquals(testUserId, createdComment.userId)
            assertNull(createdComment.password)

            // 게시글 댓글 수 증가 확인
            assertEquals(1, boardRepository.findById(testBoard.id).commentCount)

            // 이벤트 발행 확인
            verify(eventPublisher).publishCommentCreated(
                comment = argThat { id == response.id.toLong() },
                boardId = eq(testBoard.id),
                userId = eq(testUserId),
            )
            verify(theadContextUtils).getCurrentUserIdOrNull()
        }

        @Test
        @DisplayName("비로그인 사용자가 비밀번호와 함께 유효한 정보로 댓글 생성 성공")
        fun `given guest user with password and valid request, when execute, then create comment with encoded password`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(guestUserId)
            val rawPassword = "guestPassword"
            val requestDto =
                CreateComment.Request(
                    content = "비회원 사용자 댓글",
                    author = "방문자",
                    password = rawPassword,
                )
            val request = requestDto.of(testBoard.id)

            // When
            val response = createComment(request)

            // Then
            assertNotNull(response.id)
            assertEquals(request.content, response.content)

            // DB 상태 확인
            val createdComment = commentRepository.findById(response.id.toLong())
            assertEquals(request.content, createdComment.content)
            assertNull(createdComment.userId)
            assertNotNull(createdComment.password)
            assertEquals(passwordUtil.encode(rawPassword), createdComment.password)

            // 게시글 댓글 수 증가 확인
            assertEquals(1, boardRepository.findById(testBoard.id).commentCount)

            // 이벤트 발행 확인
            verify(eventPublisher).publishCommentCreated(
                comment = argThat { id == response.id.toLong() },
                boardId = eq(testBoard.id),
                userId = eq(guestUserId),
            )
            verify(theadContextUtils).getCurrentUserIdOrNull()
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @BeforeEach
        fun setup() {
            // 기본적으로 로그인 상태로 설정
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(testUserId)
        }

        @Test
        @DisplayName("댓글 내용이 비어있으면 ConstraintViolationException 발생")
        fun `given blank content, when execute, then throw ConstraintViolationException`() {
            // Given
            val requestDto = CreateComment.Request(content = " ", author = "테스터", password = null)
            val request = requestDto.of(testBoard.id)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createComment(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "content" })
            verifyNoInteractions(eventPublisher)
            assertEquals(0, boardRepository.findById(testBoard.id).commentCount) // 댓글 수 미증가
        }

        @Test
        @DisplayName("댓글 내용이 너무 길면 ConstraintViolationException 발생")
        fun `given too long content, when execute, then throw ConstraintViolationException`() {
            // Given
            val requestDto = CreateComment.Request(content = "a".repeat(CommentConstants.MAX_CONTENT_LENGTH + 1), author = "테스터", password = null)
            val request = requestDto.of(testBoard.id)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createComment(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "content" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("작성자 이름이 비어있으면 ConstraintViolationException 발생")
        fun `given blank author, when execute, then throw ConstraintViolationException`() {
            // Given
            val requestDto = CreateComment.Request(content = "정상 내용", author = " ", password = null)
            val request = requestDto.of(testBoard.id)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createComment(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "author" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비밀번호가 너무 짧으면 ConstraintViolationException 발생 (비회원)")
        fun `given guest user and too short password, when execute, then throw ConstraintViolationException`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(guestUserId)
            val requestDto = CreateComment.Request(content = "정상 내용", author = "방문자", password = "123") // 길이 부족
            val request = requestDto.of(testBoard.id)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createComment(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString().contains("password") }) // 경로 확인
            verifyNoInteractions(eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 로직 오류")
    inner class LogicFailCases {
        @Test
        @DisplayName("비로그인 사용자가 비밀번호 없이 생성 시 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user without password, when execute, then throw AppException`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(guestUserId)
            val requestDto = CreateComment.Request(content = "내용", author = "방문자", password = null) // 비밀번호 없음
            val request = requestDto.of(testBoard.id)

            // When & Then
            val exception = assertThrows<AppException> { createComment(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호 설정이 필수"))
            verifyNoInteractions(eventPublisher)
            assertEquals(
                0,
                commentRepository
                    .findAll(
                        pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
                    ).content.size,
            ) // 댓글 생성 안됨
        }

        @Test
        @DisplayName("존재하지 않는 게시글 ID로 생성 시 EntityNotFoundException 발생 (CommentService 레벨)")
        fun `given non-existent boardId, when execute, then throw EntityNotFoundException`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(testUserId) // 로그인 상태
            val nonExistentBoardId = 999L
            val requestDto = CreateComment.Request(content = "내용", author = "멤버", password = null)
            val request = requestDto.of(nonExistentBoardId) // 존재하지 않는 게시글 ID

            // When & Then
            assertThrows<EntityNotFoundException> { createComment(request) }
            verifyNoInteractions(eventPublisher)
        }
    }
}
