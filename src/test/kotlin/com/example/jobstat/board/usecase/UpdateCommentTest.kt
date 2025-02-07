package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.BoardFixture
import com.example.jobstat.board.fake.CommentFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.board.internal.service.CommentServiceImpl
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DisplayName("UpdateComment Usecase 테스트")
class UpdateCommentTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var commentService: CommentService
    private lateinit var updateComment: UpdateComment
    private lateinit var passwordUtil: FakePasswordUtil
    private var testBoardId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        passwordUtil = FakePasswordUtil()
        updateComment = UpdateComment(commentService, passwordUtil, Validation.buildDefaultValidatorFactory().validator)

        // 생성용 게시글
        val board = BoardFixture.aBoard().create()
        testBoardId = boardRepository.save(board).id
    }

    @Test
    @DisplayName("댓글 수정에 성공한다")
    fun updateCommentSuccessfully() {
        val comment = CommentFixture.aComment().withBoard(boardRepository.findById(testBoardId)).create()
        val savedComment = commentService.createComment(testBoardId, comment.content, comment.author, passwordUtil.encode("pass123"))
        val request =
            UpdateComment.Request(
                commentId = savedComment.id,
                content = "Updated Comment",
                password = "pass123",
            )
        val response = updateComment(request)
        assertEquals("Updated Comment", response.content)
    }

    @Test
    @DisplayName("잘못된 비밀번호로 댓글 수정 시도시 실패한다")
    fun failToUpdateCommentWithWrongPassword() {
        val comment = CommentFixture.aComment().withBoard(boardRepository.findById(testBoardId)).create()
        val savedComment = commentService.createComment(testBoardId, comment.content, comment.author, passwordUtil.encode("pass123"))
        val request =
            UpdateComment.Request(
                commentId = savedComment.id,
                content = "Updated Comment",
                password = "wrongPass",
            )
        assertFailsWith<IllegalArgumentException> {
            updateComment(request)
        }
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시도시 실패한다")
    fun failToUpdateNonExistentComment() {
        val request =
            UpdateComment.Request(
                commentId = 999L,
                content = "Updated Comment",
                password = "pass123",
            )
        assertFailsWith<EntityNotFoundException> {
            updateComment(request)
        }
    }
}
