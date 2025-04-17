package com.example.jobstat.community.comment.usecase.command

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.BoardFixture
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.repository.FakeCommentRepository
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.comment.service.CommentServiceImpl
import com.example.jobstat.community.event.CommunityCommandEventPublisher // Mock 대상
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.SecurityUtils // Mock 대상
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*
import kotlin.properties.Delegates

@DisplayName("UpdateComment UseCase 테스트")
class UpdateCommentUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var passwordUtil: FakePasswordUtil

    private lateinit var commentService: CommentService

    private lateinit var securityUtils: SecurityUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher

    private lateinit var updateComment: UpdateComment

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private lateinit var memberComment: Comment
    private lateinit var guestComment: Comment
    private val ownerUserId = 1L
    private val otherUserId = 2L
    private val adminUserId = 99L
    private val guestUserId: Long? = null
    private val correctPassword = "guestPassword"
    private val wrongPassword = "wrongPassword"

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        passwordUtil = FakePasswordUtil()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        securityUtils = mock()
        eventPublisher = mock()

        updateComment =
            UpdateComment(
                commentService = commentService,
                passwordUtil = passwordUtil,
                securityUtils = securityUtils,
                eventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        memberComment = commentService.createComment(testBoard.id, "원본 멤버 댓글", "멤버", null, ownerUserId)
        guestComment = commentService.createComment(testBoard.id, "원본 비회원 댓글", "게스트", passwordUtil.encode(correctPassword), guestUserId)
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
        @DisplayName("로그인 사용자가 자신의 댓글 수정 성공")
        fun `given owner user and valid request, when update own comment, then success and publish event`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(ownerUserId)
            whenever(securityUtils.isAdmin()).thenReturn(false)
            val newContent = "수정된 멤버 댓글 내용"
            val requestDto = UpdateComment.Request(content = newContent, password = null)
            val request = requestDto.of(memberComment.id)

            // When
            val response = updateComment(request)

            // Then
            assertEquals(memberComment.id.toString(), response.id)
            assertEquals(newContent, response.content)
            assertNotNull(response.updatedAt)

            // DB 확인
            val updatedComment = commentRepository.findById(memberComment.id)
            assertEquals(newContent, updatedComment.content)

            // Verify
            verify(eventPublisher).publishCommentUpdated(
                comment = argThat { id == memberComment.id && content == newContent },
                boardId = eq(testBoard.id),
            )
            verify(securityUtils).getCurrentUserId()
        }

        @Test
        @DisplayName("관리자가 다른 사용자 댓글 수정 성공")
        fun `given admin user, when update other user comment, then success`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(adminUserId)
            whenever(securityUtils.isAdmin()).thenReturn(true)
            val newContent = "관리자가 수정한 댓글"
            val requestDto = UpdateComment.Request(content = newContent, password = null)
            val request = requestDto.of(memberComment.id)

            // When
            val response = updateComment(request)

            // Then
            assertEquals(newContent, response.content)
            val updatedComment = commentRepository.findById(memberComment.id)
            assertEquals(newContent, updatedComment.content)
            verify(eventPublisher).publishCommentUpdated(any(), eq(testBoard.id))
        }

        @Test
        @DisplayName("비회원 사용자가 올바른 비밀번호로 댓글 수정 성공")
        fun `given guest user with correct password, when update guest comment, then success`() {
            // Given
            val newContent = "비회원 수정 댓글 내용"
            val requestDto = UpdateComment.Request(content = newContent, password = correctPassword)
            val request = requestDto.of(guestComment.id)

            // When
            val response = updateComment(request)

            // Then
            assertEquals(newContent, response.content)
            val updatedComment = commentRepository.findById(guestComment.id)
            assertEquals(newContent, updatedComment.content)

            assertTrue(passwordUtil.matches(correctPassword, updatedComment.password!!))
            verify(eventPublisher).publishCommentUpdated(any(), eq(testBoard.id))
            verifyNoInteractions(securityUtils)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 권한 없음")
    inner class PermissionFailCases {
        @Test
        @DisplayName("로그인 사용자가 다른 사용자 댓글 수정 시 AppException(INSUFFICIENT_PERMISSION) 발생")
        fun `given non-owner user, when update other user comment, then throw AppException`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(otherUserId)
            whenever(securityUtils.isAdmin()).thenReturn(false)
            val requestDto = UpdateComment.Request("수정 시도", null)
            val request = requestDto.of(memberComment.id)

            // When & Then
            val exception = assertThrows<AppException> { updateComment(request) }
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.errorCode)
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비회원 댓글 수정 시 비밀번호 틀리면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user with wrong password, when update guest comment, then throw AppException`() {
            // Given
            val requestDto = UpdateComment.Request("수정 시도", wrongPassword)
            val request = requestDto.of(guestComment.id)

            // When & Then
            val exception = assertThrows<AppException> { updateComment(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 일치하지 않습니다"))
            verifyNoInteractions(eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        private var commentId by Delegates.notNull<Long>()

        @BeforeEach
        fun setupValidationCase() {
            commentId = memberComment.id

            whenever(securityUtils.getCurrentUserId()).thenReturn(ownerUserId)
            whenever(securityUtils.isAdmin()).thenReturn(false)
        }

        @Test
        @DisplayName("댓글 내용이 비어있으면 ConstraintViolationException 발생")
        fun `given blank content, when update comment, then throw ConstraintViolationException`() {
            // Given
            val requestDto = UpdateComment.Request(content = " ", password = null)
            val request = requestDto.of(commentId)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { updateComment(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "content" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("댓글 내용이 너무 길면 ConstraintViolationException 발생")
        fun `given too long content, when update comment, then throw ConstraintViolationException`() {
            // Given
            val requestDto = UpdateComment.Request(content = "a".repeat(1001), password = null)
            val request = requestDto.of(commentId)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { updateComment(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "content" })
            verifyNoInteractions(eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 기타")
    inner class OtherFailCases {
        @Test
        @DisplayName("존재하지 않는 댓글 수정 시 EntityNotFoundException 발생 (CommentService 레벨)")
        fun `given non-existent commentId, when update comment, then throw EntityNotFoundException`() {
            // Given
            val nonExistentCommentId = 999L
            whenever(securityUtils.getCurrentUserId()).thenReturn(ownerUserId) // 권한은 있음
            val requestDto = UpdateComment.Request("내용", null)
            val request = requestDto.of(nonExistentCommentId)

            // When & Then
            assertThrows<EntityNotFoundException> { updateComment(request) }
            verifyNoInteractions(eventPublisher)
        }
    }
}
