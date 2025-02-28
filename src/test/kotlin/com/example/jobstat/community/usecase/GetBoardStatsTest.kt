package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.board.usecase.GetBoardStats
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.comment.service.CommentServiceImpl
import com.example.jobstat.community.fake.BoardFixture
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("GetBoardStats Usecase 테스트")
class GetBoardStatsTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var getBoardStats: GetBoardStats
    private var testBoardId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        getBoardStats = GetBoardStats(boardService, commentService, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        val savedCategory = categoryRepository.save(category)
        val board = BoardFixture.aBoard().create()
        testBoardId =
            boardService
                .createBoard(
                    title = board.title,
                    content = board.content,
                    author = "authorStats",
                    categoryId = savedCategory.id,
                    password = null,
                ).id

        // authorStats가 작성한 게시글 추가
        repeat(2) {
            boardService.createBoard(
                title = "Extra Title $it",
                content = "Extra Content $it",
                author = "authorStats",
                categoryId = savedCategory.id,
                password = null,
            )
        }
        // 댓글 추가
        commentService.createComment(testBoardId, "Comment", "authorStats", null, null)
    }

    @Test
    @DisplayName("게시글 통계 계산에 성공한다")
    fun calculateBoardStatsSuccessfully() {
        val request = GetBoardStats.Request(author = "authorStats", boardId = testBoardId)
        val response = getBoardStats(request)
        // authorStats 총 게시글 수 = 3, 댓글 존재 여부 = true
        assertEquals(3, response.totalBoardCount)
        assertTrue(response.hasCommentedOnBoard)
    }
}
