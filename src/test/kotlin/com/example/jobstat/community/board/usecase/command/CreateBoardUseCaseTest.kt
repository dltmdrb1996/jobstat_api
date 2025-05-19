package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.community.event.CommunityCommandEventPublisher // Mock 대상
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_security.util.SecurityUtils // Mock 대상
import com.example.jobstat.utils.FakePasswordUtil // Fake 구현 사용
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*
import org.springframework.data.domain.Pageable

@DisplayName("CreateBoard UseCase 테스트")
class CreateBoardUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var passwordUtil: FakePasswordUtil

    private lateinit var boardService: BoardService

    private lateinit var securityUtils: SecurityUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher // Mock 처리

    private lateinit var createBoard: CreateBoard

    private lateinit var testCategory: BoardCategory
    private val testUserId = 1L
    private val guestUserId: Long? = null

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        passwordUtil = FakePasswordUtil()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        securityUtils = mock()
        eventPublisher = mock()

        createBoard =
            CreateBoard(
                boardService = boardService,
                securityUtils = securityUtils,
                passwordUtil = passwordUtil,
                communityCommandEventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory =
            categoryRepository.save(
                CategoryFixture.aCategory().withName("TEST_CAT").create(),
            )
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("로그인 사용자가 유효한 정보로 게시글 생성 성공")
        fun `given logged in user and valid request, when execute, then create board and publish event`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)

            val request =
                CreateBoard.Request(
                    title = "로그인 사용자 제목",
                    content = "로그인 사용자 내용입니다. 충분히 깁니다.",
                    author = "로그인유저",
                    categoryId = testCategory.id,
                    password = null, // 로그인 시 비밀번호 불필요
                )

            // When
            val response = createBoard(request)

            // Then
            assertNotNull(response.id)
            assertEquals(request.title, response.title)
            assertNotNull(response.createdAt)

            // DB 상태 확인
            val createdBoard = boardRepository.findById(response.id.toLong())
            assertEquals(request.title, createdBoard.title)
            assertEquals(request.content, createdBoard.content)
            assertEquals(request.author, createdBoard.author)
            assertEquals(testCategory.id, createdBoard.category.id)
            assertEquals(testUserId, createdBoard.userId)
            assertNull(createdBoard.password)

            // 이벤트 발행 확인
            verify(eventPublisher).publishBoardCreated(
                board = argThat { id == response.id.toLong() },
                categoryId = eq(testCategory.id),
                userId = eq(testUserId),
            )
            verify(securityUtils).getCurrentUserId() // securityUtils 호출 확인
        }

        @Test
        @DisplayName("비로그인 사용자가 비밀번호와 함께 유효한 정보로 게시글 생성 성공")
        fun `given guest user with password and valid request, when execute, then create board and publish event`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId) // 비로그인 상태 Mock
            val rawPassword = "gPassword123"

            val request =
                CreateBoard.Request(
                    title = "비회원 사용자 제목",
                    content = "비회원 사용자 내용입니다. 충분히 깁니다.",
                    author = "비회원",
                    categoryId = testCategory.id,
                    password = rawPassword,
                )

            // When
            val response = createBoard(request)

            // Then
            assertNotNull(response.id)
            assertEquals(request.title, response.title)
            assertNotNull(response.createdAt)

            // DB 상태 확인
            val createdBoard = boardRepository.findById(response.id.toLong())
            assertEquals(request.title, createdBoard.title)
            assertEquals(request.content, createdBoard.content)
            assertEquals(request.author, createdBoard.author)
            assertEquals(testCategory.id, createdBoard.category.id)
            assertNull(createdBoard.userId)
            assertNotNull(createdBoard.password)
            assertEquals(passwordUtil.encode(rawPassword), createdBoard.password)

            // 이벤트 발행 확인
            verify(eventPublisher).publishBoardCreated(
                board = argThat { id == response.id.toLong() },
                categoryId = eq(testCategory.id),
                userId = eq(guestUserId),
            )
            verify(securityUtils).getCurrentUserId()
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @BeforeEach
        fun setup() {
            // 기본적으로 로그인 상태로 설정
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
        }

        @Test
        @DisplayName("제목이 너무 짧으면 ConstraintViolationException 발생")
        fun `given too short title, when execute, then throw ConstraintViolationException`() {
            // Given
            val request =
                CreateBoard.Request(
                    title = "짧",
                    content = "내용은 충분히 깁니다.",
                    author = "작성자",
                    categoryId = testCategory.id,
                    password = null,
                )

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createBoard(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "title" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("내용이 너무 길면 ConstraintViolationException 발생")
        fun `given too long content, when execute, then throw ConstraintViolationException`() {
            // Given
            val request =
                CreateBoard.Request(
                    title = "적절한 제목",
                    content = "a".repeat(BoardConstants.MAX_CONTENT_LENGTH + 1),
                    author = "작성자",
                    categoryId = testCategory.id,
                    password = null,
                )

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createBoard(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "content" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("작성자 이름이 비어있으면 ConstraintViolationException 발생")
        fun `given blank author, when execute, then throw ConstraintViolationException`() {
            // Given
            val request =
                CreateBoard.Request(
                    title = "적절한 제목",
                    content = "내용은 충분합니다.",
                    author = " ", // 공백
                    categoryId = testCategory.id,
                    password = null,
                )

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createBoard(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "author" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비밀번호가 너무 짧으면 ConstraintViolationException 발생 (비회원)")
        fun `given guest user and too short password, when execute, then throw ConstraintViolationException`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId)
            val request =
                CreateBoard.Request(
                    title = "적절한 제목",
                    content = "내용은 충분합니다.",
                    author = "비회원",
                    categoryId = testCategory.id,
                    password = "123",
                )

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { createBoard(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "password" })
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
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId) // 비로그인 상태

            val request =
                CreateBoard.Request(
                    title = "비회원 게시글",
                    content = "내용은 충분합니다.",
                    author = "비회원",
                    categoryId = testCategory.id,
                    password = null, // 비밀번호 없음
                )

            // When & Then
            val exception = assertThrows<AppException> { createBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호 설정이 필수"))
            verifyNoInteractions(eventPublisher)
            assertEquals(
                0,
                boardRepository
                    .findAll(
                        Pageable.ofSize(10),
                    ).content.size,
            )
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 ID로 생성 시 EntityNotFoundException 발생 (BoardService 레벨)")
        fun `given non-existent categoryId, when execute, then throw EntityNotFoundException`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId) // 로그인 상태 (누구나 가능)
            val nonExistentCategoryId = 999L

            val request =
                CreateBoard.Request(
                    title = "제목",
                    content = "내용입니다.",
                    author = "작성자",
                    categoryId = nonExistentCategoryId,
                    password = null,
                )

            // When & Then
            assertThrows<ConstraintViolationException> { createBoard(request) }
            verifyNoInteractions(eventPublisher)
            assertEquals(
                0,
                boardRepository
                    .findAll(
                        Pageable.ofSize(10),
                    ).content.size,
            )
        }
    }
}
