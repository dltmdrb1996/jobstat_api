package com.wildrew.jobstat.community.comment.usecase.command

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.repository.FakeCommentRepository
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.service.CommentServiceImpl
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.community.utils.FakePasswordUtil
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*

@DisplayName("DeleteComment UseCase 테스트")
class DeleteCommentUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var passwordUtil: FakePasswordUtil

    private lateinit var commentService: CommentService

    private lateinit var theadContextUtils: TheadContextUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher

    private lateinit var deleteComment: DeleteComment

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private lateinit var memberComment: Comment
    private lateinit var guestComment: Comment
    private val ownerUserId = 1L
    private val otherUserId = 2L
    private val adminUserId = 99L
    private val guestUserId: Long? = null
    private val correctPassword = "gPassword"
    private val wrongPassword = "wPassword"

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        passwordUtil = FakePasswordUtil()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        theadContextUtils = mock()
        eventPublisher = mock()

        deleteComment =
            DeleteComment(
                commentService = commentService,
                passwordUtil = passwordUtil,
                theadContextUtils = theadContextUtils,
                communityCommandEventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        memberComment =
            commentService.createComment(
                testBoard.id,
                "멤버 댓글",
                "멤버",
                null,
                ownerUserId,
            )
        guestComment =
            commentService.createComment(
                testBoard.id,
                "비회원 댓글",
                "게스트",
                password = passwordUtil.encode(correctPassword),
                guestUserId,
            )

        assertEquals(2, boardRepository.findById(testBoard.id).commentCount)
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
        @DisplayName("로그인 사용자가 자신의 댓글 삭제 성공")
        fun `given owner user, when delete own comment, then success and publish event`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(ownerUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(false)
            val requestDto = DeleteComment.Request(password = null)
            val request = requestDto.of(memberComment.id)

            // When
            val response = deleteComment(request)

            // Then
            assertTrue(response.success)
            assertFalse(commentRepository.existsById(memberComment.id))
            assertEquals(1, boardRepository.findById(testBoard.id).commentCount)

            verify(eventPublisher).publishCommentDeleted(memberComment.id, testBoard.id)
            verify(theadContextUtils).getCurrentUserIdOrNull()
            verify(theadContextUtils).isAdmin()
        }

        @Test
        @DisplayName("관리자가 다른 사용자의 댓글 삭제 성공")
        fun `given admin user, when delete other user comment, then success`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(adminUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(true)
            val requestDto = DeleteComment.Request(password = null)
            val request = requestDto.of(memberComment.id)

            // When
            val response = deleteComment(request)

            // Then
            assertTrue(response.success)
            assertFalse(commentRepository.existsById(memberComment.id))
            assertEquals(1, boardRepository.findById(testBoard.id).commentCount)
            verify(eventPublisher).publishCommentDeleted(memberComment.id, testBoard.id)
        }

        @Test
        @DisplayName("비회원 사용자가 올바른 비밀번호로 댓글 삭제 성공")
        fun `given guest user with correct password, when delete guest comment, then success`() {
            // Given
            val requestDto = DeleteComment.Request(password = correctPassword)
            val request = requestDto.of(guestComment.id)

            // When
            val response = deleteComment(request)

            // Then
            assertTrue(response.success)
            assertFalse(commentRepository.existsById(guestComment.id))
            assertEquals(1, boardRepository.findById(testBoard.id).commentCount)

            assertTrue(passwordUtil.matches(correctPassword, guestComment.password!!))
            verify(eventPublisher).publishCommentDeleted(guestComment.id, testBoard.id)
            verifyNoInteractions(theadContextUtils)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 권한 없음")
    inner class PermissionFailCases {
        @Test
        @DisplayName("로그인 사용자가 다른 사용자 댓글 삭제 시 AppException(INSUFFICIENT_PERMISSION) 발생")
        fun `given non-owner user, when delete other user comment, then throw AppException`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(otherUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(false)
            val requestDto = DeleteComment.Request(password = null)
            val request = requestDto.of(memberComment.id)

            // When & Then
            val exception = assertThrows<AppException> { deleteComment(request) }
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.errorCode)
            assertTrue(commentRepository.existsById(memberComment.id))
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비회원 댓글 삭제 시 비밀번호 틀리면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user with wrong password, when delete guest comment, then throw AppException`() {
            // Given
            val requestDto = DeleteComment.Request(password = wrongPassword)
            val request = requestDto.of(guestComment.id)

            // When & Then
            val exception = assertThrows<AppException> { deleteComment(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 일치하지 않습니다"))
            assertTrue(commentRepository.existsById(guestComment.id))
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비회원 댓글 삭제 시 비밀번호 미입력 시 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user without password, when delete guest comment, then throw AppException`() {
            // Given
            val requestDto = DeleteComment.Request(password = null)
            val request = requestDto.of(guestComment.id)

            // When & Then
            val exception = assertThrows<AppException> { deleteComment(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 필요합니다"))
            assertTrue(commentRepository.existsById(guestComment.id))
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("로그인 필요한 댓글 삭제 시 비로그인 상태면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user, when delete member comment, then throw AppException`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(guestUserId)
            val requestDto = DeleteComment.Request(password = null)
            val request = requestDto.of(memberComment.id)

            // When & Then
            val exception = assertThrows<AppException> { deleteComment(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("로그인이 필요합니다"))
            assertTrue(commentRepository.existsById(memberComment.id))
            verifyNoInteractions(eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 기타")
    inner class OtherFailCases {
        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시 EntityNotFoundException 발생 (CommentService 레벨)")
        fun `given non-existent commentId, when delete comment, then throw EntityNotFoundException`() {
            // Given
            val nonExistentCommentId = 999L
            whenever(theadContextUtils.getCurrentUserIdOrNull()).thenReturn(ownerUserId)
            val requestDto = DeleteComment.Request(password = null)
            val request = requestDto.of(nonExistentCommentId)

            // When & Then
            assertThrows<EntityNotFoundException> { deleteComment(request) }
            verifyNoInteractions(eventPublisher)
        }
    }
}
