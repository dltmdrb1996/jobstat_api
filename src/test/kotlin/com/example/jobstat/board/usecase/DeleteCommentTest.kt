package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.BoardFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.BoardServiceImpl
import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.board.internal.service.CommentServiceImpl
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("DeleteComment Usecase 테스트")
class DeleteCommentTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var deleteComment: DeleteComment
    private var testBoardId by Delegates.notNull<Long>()
    private lateinit var passwordUtil: FakePasswordUtil

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        passwordUtil = FakePasswordUtil()
        deleteComment = DeleteComment(commentService, passwordUtil, Validation.buildDefaultValidatorFactory().validator)
        // Create a board first
        val category =
            com.example.jobstat.board.fake.CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Category 1")
                .withDescription("Desc 1")
                .create()
        categoryRepository.save(category)
        val board = BoardFixture.aBoard().withCategory(category).create()
        testBoardId =
            boardService
                .createBoard(
                    title = board.title,
                    content = board.content,
                    author = board.author,
                    categoryId = category.id,
                    password = passwordUtil.encode("1234"),
                ).id
    }

    @Test
    @DisplayName("댓글 삭제에 성공한다")
    fun deleteCommentSuccessfully() {
        val comment =
            commentService.createComment(
                boardId = testBoardId,
                content = "Delete Comment",
                author = "testUser",
                password = passwordUtil.encode("pass123"),
            )
        val request = DeleteComment.Request(commentId = comment.id, password = "pass123")
        val response = deleteComment(request)
        assertTrue(response.success)
        assertFailsWith<EntityNotFoundException> { commentService.getCommentById(comment.id) }
    }
}
