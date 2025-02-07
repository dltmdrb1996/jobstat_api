package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.BoardServiceImpl
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("GetBoardList Usecase 테스트")
class GetBoardListTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var boardService: BoardService
    private lateinit var getBoardList: GetBoardList
    private var testCategoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository, FakeCommentRepository())
        getBoardList = GetBoardList(boardService, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        testCategoryId = categoryRepository.save(category).id

        repeat(25) { idx ->
            boardService.createBoard(
                title = "Title $idx",
                content = "Content $idx",
                author = "user$idx",
                categoryId = testCategoryId,
                password = null,
            )
        }
    }

    @Test
    @DisplayName("전체 게시글 페이징 조회에 성공한다")
    fun retrieveBoardListWithPagingSuccessfully() {
        val request = GetBoardList.Request(page = 0, categoryId = null, author = null, keyword = null)
        val response = getBoardList(request)
        assertEquals(20, response.items.content.size)
        assertEquals(25, response.totalCount)
        assertTrue(response.hasNext)
    }
}
