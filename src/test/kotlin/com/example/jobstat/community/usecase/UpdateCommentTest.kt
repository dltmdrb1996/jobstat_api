package com.example.jobstat.community.usecase

import com.example.jobstat.community.fake.BoardFixture
import com.example.jobstat.community.fake.CommentFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import com.example.jobstat.community.internal.service.CommentService
import com.example.jobstat.community.internal.service.CommentServiceImpl
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.utils.SecurityUtils
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DisplayName("UpdateComment Usecase 테스트")
class UpdateCommentTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var commentService: CommentService
    private lateinit var updateComment: UpdateComment
    private lateinit var securityUtils: SecurityUtils
    private lateinit var passwordUtil: FakePasswordUtil
    private var testBoardId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        securityUtils = mock() // 모의 객체 생성
        passwordUtil = FakePasswordUtil()
        updateComment = UpdateComment(commentService, passwordUtil, securityUtils, Validation.buildDefaultValidatorFactory().validator)

        // 댓글 생성용 게시글 생성
        val board = BoardFixture.aBoard().create()
        testBoardId = boardRepository.save(board).id
    }

    @Test
    @DisplayName("댓글 수정에 성공한다 (비회원 댓글)")
    fun updateCommentSuccessfully_Guest() {
        val encodedPassword = passwordUtil.encode("pass123")
        val comment =
            CommentFixture
                .aComment()
                .withBoard(boardRepository.findById(testBoardId))
                .create()
        val savedComment =
            commentService.createComment(
                boardId = testBoardId,
                content = comment.content,
                author = comment.author,
                password = encodedPassword,
                userId = null,
            )
        val request =
            UpdateComment.ExecuteRequest(
                commentId = savedComment.id,
                content = "Updated Comment",
                password = "pass123",
            )
        val response = updateComment(request)
        assertEquals("Updated Comment", response.content)
    }

    @Test
    @DisplayName("잘못된 비밀번호로 댓글 수정 시도시 실패한다 (비회원 댓글)")
    fun failToUpdateCommentWithWrongPassword_Guest() {
        val encodedPassword = passwordUtil.encode("pass123")
        val comment =
            CommentFixture
                .aComment()
                .withBoard(boardRepository.findById(testBoardId))
                .create()
        val savedComment =
            commentService.createComment(
                boardId = testBoardId,
                content = comment.content,
                author = comment.author,
                password = encodedPassword,
                userId = null,
            )
        val request =
            UpdateComment.ExecuteRequest(
                commentId = savedComment.id,
                content = "Updated Comment",
                password = "wrongPass",
            )
        assertFailsWith<AppException> {
            updateComment(request)
        }
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시도시 실패한다")
    fun failToUpdateNonExistentComment() {
        val request =
            UpdateComment.ExecuteRequest(
                commentId = 999L,
                content = "Updated Comment",
                password = "pass123",
            )
        assertFailsWith<EntityNotFoundException> {
            updateComment(request)
        }
    }

    @Test
    @DisplayName("댓글 수정에 성공한다 (회원 댓글)")
    fun updateCommentSuccessfully_Member() {
        whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
        val comment =
            CommentFixture
                .aComment()
                .withBoard(boardRepository.findById(testBoardId))
                .create()
        val savedComment =
            commentService.createComment(
                boardId = testBoardId,
                content = comment.content,
                author = comment.author,
                password = null,
                userId = 1L,
            )
        val request =
            UpdateComment.ExecuteRequest(
                commentId = savedComment.id,
                content = "Member Updated Comment",
                password = null,
            )
        val response = updateComment(request)
        assertEquals("Member Updated Comment", response.content)
    }

    @Test
    @DisplayName("비로그인 사용자가 비밀번호 없이 댓글 수정 시도시 실패한다 (회원 댓글)")
    fun updateCommentWithoutPasswordAndWithoutUserIdFails() {
        // 회원 댓글의 경우, securityUtils.getCurrentUserId()가 null이면 인증 실패
        whenever(securityUtils.getCurrentUserId()).thenReturn(null)
        val comment =
            CommentFixture
                .aComment()
                .withBoard(boardRepository.findById(testBoardId))
                .create()
        // 여기서는 회원 댓글로 생성되었으므로 userId를 임의로 2L로 설정
        val savedComment =
            commentService.createComment(
                boardId = testBoardId,
                content = comment.content,
                author = comment.author,
                password = null,
                userId = 2L,
            )
        val request =
            UpdateComment.ExecuteRequest(
                commentId = savedComment.id,
                content = "Updated Comment",
                password = null,
            )
        assertFailsWith<AppException> {
            updateComment(request)
        }
    }
}
