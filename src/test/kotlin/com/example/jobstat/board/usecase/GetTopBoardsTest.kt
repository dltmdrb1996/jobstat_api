package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.BoardFixture
import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.BoardServiceImpl
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("GetTopBoards Usecase 테스트")
class GetTopBoardsTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var getTopBoards: GetTopBoards
    private var testCategoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        getTopBoards = GetTopBoards(boardService, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        testCategoryId = categoryRepository.save(category).id

        // 생성: board1 (5 views), board2 (10 views), board3 (3 views)
        val board1 = BoardFixture.aBoard().withViewCount(5).create()
        val board2 = BoardFixture.aBoard().withViewCount(10).create()
        val board3 = BoardFixture.aBoard().withViewCount(3).create()

        boardService.createBoard(board1.title, board1.content, board1.author, testCategoryId, board1.password)
        boardService.createBoard(board2.title, board2.content, board2.author, testCategoryId, board2.password)
        boardService.createBoard(board3.title, board3.content, board3.author, testCategoryId, board3.password)
    }

    @Test
    @DisplayName("조회수 기준 상위 2개 게시글이 반환된다")
    fun retrieveTopTwoBoardsByViewCount() {
        val request = GetTopBoards.Request(limit = 2)
        val response = getTopBoards(request)
        assertEquals(2, response.items.size)
        // 첫 번째는 10회, 두 번째는 5회
        assertTrue(response.items[0].viewCount >= response.items[1].viewCount)
    }
}
