package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.BoardFixture
import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.BoardServiceImpl
import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.board.internal.service.CommentServiceImpl
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("AddCommentToBoard Usecase 테스트")
class AddCommentToBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var addCommentToBoard: AddCommentToBoard
    private var testBoardId by Delegates.notNull<Long>()

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository, commentRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        addCommentToBoard = AddCommentToBoard(boardService, commentService, Validation.buildDefaultValidatorFactory().validator)

        // 미리 카테고리 및 게시글 생성
        val category =
            CategoryFixture
                .aCategory()
                .withName("TEST_CAT")
                .withDisplayName("Test Category")
                .withDescription("Test Desc")
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
                    password = board.password,
                ).id
    }

    @Test
    @DisplayName("유효한 요청으로 게시글에 댓글 추가에 성공한다")
    fun addCommentToBoardWithValidRequest() {
        val request =
            AddCommentToBoard.Request(
                boardId = testBoardId,
                content = "Great article!",
                author = "commenter",
            )
        val response = addCommentToBoard(request)
        assertNotNull(response.commentId)
        assertEquals(testBoardId, response.boardId)
        assertEquals("Great article!", response.content)
        assertEquals("commenter", response.author)
        assertNotNull(response.createdAt)
    }
}
