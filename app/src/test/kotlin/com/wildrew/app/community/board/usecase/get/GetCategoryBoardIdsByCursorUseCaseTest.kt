package com.wildrew.app.community.board.usecase.get

import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.fixture.CategoryFixture
import com.wildrew.app.community.board.repository.FakeBoardRepository
import com.wildrew.app.community.board.repository.FakeCategoryRepository
import com.wildrew.app.community.board.service.BoardService
import com.wildrew.app.community.board.service.BoardServiceImpl
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.Pageable

@DisplayName("GetCategoryBoardIdsByCursorUseCase 테스트")
class GetCategoryBoardIdsByCursorUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getCategoryBoardIdsByCursor: GetCategoryBoardIdsByCursorUseCase

    private lateinit var category1: BoardCategory
    private lateinit var category2: BoardCategory
    private val testLimit = 5
    private lateinit var category1BoardIdsDesc: List<Long> // 카테고리1 게시글 ID (내림차순)

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getCategoryBoardIdsByCursor =
            GetCategoryBoardIdsByCursorUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        category1 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT1").create())
        category2 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT2").create())
        val cat1Ids = mutableListOf<Long>()
        repeat(12) { cat1Ids.add(boardService.createBoard("C1 T $it", "C $it", "Author", category1.id, null, (it + 1).toLong()).id) }
        repeat(3) { boardService.createBoard("C2 T $it", "C $it", "Author", category2.id, null, (it + 100).toLong()) }
        cat1Ids.reverse()
        category1BoardIdsDesc = cat1Ids
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("카테고리1 첫 페이지 조회 (lastBoardId = null)")
        fun `given category1 and null cursor, when execute, then return first page ids`() {
            // Given
            val request = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = category1.id, lastBoardId = null, limit = testLimit)
            val expectedIds = category1BoardIdsDesc.take(testLimit).map { it.toString() }

            // When
            val response = getCategoryBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("카테고리1 두 번째 페이지 조회")
        fun `given category1 and cursor from first page, when execute, then return second page ids`() {
            // Given
            val lastId = category1BoardIdsDesc[testLimit - 1]
            val request = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = category1.id, lastBoardId = lastId, limit = testLimit)
            val expectedIds = category1BoardIdsDesc.drop(testLimit).take(testLimit).map { it.toString() }

            // When
            val response = getCategoryBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("카테고리1 마지막 페이지 조회")
        fun `given category1 and cursor from second page, when execute, then return last page ids`() {
            // Given
            val lastId = category1BoardIdsDesc[testLimit * 2 - 1]
            val request = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = category1.id, lastBoardId = lastId, limit = testLimit)
            val expectedIds = category1BoardIdsDesc.drop(testLimit * 2).take(testLimit).map { it.toString() } // 2개 남음

            // When
            val response = getCategoryBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(2, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("카테고리2 첫 페이지 조회 (게시글 3개)")
        fun `given category2 and null cursor, when execute, then return all 3 ids`() {
            // Given
            val request = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = category2.id, lastBoardId = null, limit = testLimit)
            val expectedIds =
                boardRepository
                    .findAll(
                        Pageable.ofSize(testLimit),
                    ).filter { it.category.id == category2.id }
                    .sortedByDescending { it.id }
                    .map { it.id.toString() }

            // When
            val response = getCategoryBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(3, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("데이터 없는 카테고리 조회 시 빈 목록 반환")
        fun `given category with no boards, when execute, then return empty list`() {
            // Given
            val category3 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT3").create())
            val request = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = category3.id, lastBoardId = null)

            // When
            val response = getCategoryBoardIdsByCursor(request)

            // Then
            assertTrue(response.ids.isEmpty())
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @Test
        @DisplayName("카테고리 ID가 0 이하면 ConstraintViolationException 발생")
        fun `given non-positive categoryId, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = 0, lastBoardId = null)
            val request2 = GetCategoryBoardIdsByCursorUseCase.Request(categoryId = -1, lastBoardId = null)
            // When & Then
            assertThrows<ConstraintViolationException> { getCategoryBoardIdsByCursor(request1) }
            assertThrows<ConstraintViolationException> { getCategoryBoardIdsByCursor(request2) }
        }
    }
}
