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

@DisplayName("AddComment Usecase 테스트")
class AddCommentTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var addComment: AddComment
    private var testBoardId by Delegates.notNull<Long>()

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository, commentRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        addComment = AddComment(commentService, Validation.buildDefaultValidatorFactory().validator)

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
    @DisplayName("유효한 요청으로 댓글 생성에 성공한다")
    fun createCommentWithValidRequest() {
        val request =
            AddComment.Request(
                boardId = testBoardId,
                content = "좋은 글이네요!",
                author = "댓글작성자",
            )
        val response = addComment(request)
        assertNotNull(response.id)
        assertEquals("좋은 글이네요!", response.content)
        assertEquals("댓글작성자", response.author)
        assertEquals(testBoardId, response.boardId)
        assertNotNull(response.createdAt)
    }
}
