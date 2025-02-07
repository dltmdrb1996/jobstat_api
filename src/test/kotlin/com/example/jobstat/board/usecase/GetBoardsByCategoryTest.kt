package com.example.jobstat.board.usecase

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

@DisplayName("GetBoardsByCategory Usecase 테스트")
class GetBoardsByCategoryTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var boardService: BoardService
    private lateinit var getBoardsByCategory: GetBoardsByCategory
    private var testCategoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository, FakeCommentRepository())
        getBoardsByCategory = GetBoardsByCategory(boardService, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        testCategoryId = categoryRepository.save(category).id

        repeat(5) { idx ->
            boardService.createBoard(
                title = "Title $idx",
                content = "Content $idx",
                author = if (idx % 2 == 0) "user1" else "user2",
                categoryId = testCategoryId,
                password = null,
            )
        }
    }

    @Test
    @DisplayName("작성자 미지정시 카테고리별 게시글 조회에 성공한다")
    fun retrieveBoardsByCategoryWithoutAuthor() {
        val request = GetBoardsByCategory.Request(categoryId = testCategoryId, author = null, page = 0)
        val response = getBoardsByCategory(request)
        assertEquals(5, response.items.totalElements)
    }

    @Test
    @DisplayName("작성자와 카테고리로 게시글 조회에 성공한다")
    fun retrieveBoardsByCategoryAndAuthor() {
        val request = GetBoardsByCategory.Request(categoryId = testCategoryId, author = "user1", page = 0)
        val response = getBoardsByCategory(request)
        // user1가 작성한 게시글 수 (idx 0,2,4 → 3개)
        assertEquals(3, response.items.totalElements)
    }
}
