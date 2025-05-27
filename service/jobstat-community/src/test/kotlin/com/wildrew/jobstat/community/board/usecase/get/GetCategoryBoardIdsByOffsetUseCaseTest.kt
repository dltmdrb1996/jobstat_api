package com.wildrew.jobstat.community.board.usecase.get

import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.board.service.BoardServiceImpl
import com.wildrew.jobstat.community.board.utils.BoardConstants
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest

@DisplayName("GetCategoryBoardIdsByOffsetUseCase 테스트")
class GetCategoryBoardIdsByOffsetUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getCategoryBoardIdsByOffset: GetCategoryBoardIdsByOffsetUseCase

    private lateinit var category1: BoardCategory
    private lateinit var category2: BoardCategory
    private val defaultPageSize = BoardConstants.DEFAULT_PAGE_SIZE
    private lateinit var category1BoardIdsDesc: List<Long> // 카테고리1의 게시글 ID 목록 (내림차순)

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getCategoryBoardIdsByOffset =
            GetCategoryBoardIdsByOffsetUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        category1 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT1").create())
        category2 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT2").create())

        val cat1Ids = mutableListOf<Long>()
        repeat(25) { cat1Ids.add(boardService.createBoard("C1 T $it", "C $it", "Author", category1.id, null, (it + 1).toLong()).id) }
        repeat(5) { boardService.createBoard("C2 T $it", "C $it", "Author", category2.id, null, (it + 100).toLong()) }
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
        @DisplayName("카테고리1의 첫 페이지 조회")
        fun `given category1 and page 0, when execute, then return first page ids for category1`() {
            // Given
            val request = GetCategoryBoardIdsByOffsetUseCase.Request(categoryId = category1.id, page = 0, size = defaultPageSize)
            val expectedIds = category1BoardIdsDesc.take(defaultPageSize).map { it.toString() }

            // When
            val response = getCategoryBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("카테고리1의 두 번째 페이지 조회")
        fun `given category1 and page 1, when execute, then return second page ids for category1`() {
            // Given
            val request = GetCategoryBoardIdsByOffsetUseCase.Request(categoryId = category1.id, page = 1, size = defaultPageSize)
            val expectedIds = category1BoardIdsDesc.drop(defaultPageSize).take(defaultPageSize).map { it.toString() } // 21번째부터 5개

            // When
            val response = getCategoryBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(5, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("카테고리2의 첫 페이지 조회 (게시글 5개)")
        fun `given category2 and page 0, when execute, then return all 5 ids for category2`() {
            // Given
            val request = GetCategoryBoardIdsByOffsetUseCase.Request(categoryId = category2.id, page = 0, size = defaultPageSize)
            val expectedIds =
                boardRepository
                    .findAll(
                        pageable = PageRequest.of(0, defaultPageSize),
                    ).filter { it.category.id == category2.id }
                    .sortedByDescending { it.id }
                    .map { it.id.toString() }

            // When
            val response = getCategoryBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(5, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("데이터 없는 카테고리 조회 시 빈 목록 반환")
        fun `given category with no boards, when execute, then return empty list`() {
            // Given
            val category3 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT3").create())
            val request = GetCategoryBoardIdsByOffsetUseCase.Request(categoryId = category3.id, page = 0)

            // When
            val response = getCategoryBoardIdsByOffset(request)

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
            val request1 = GetCategoryBoardIdsByOffsetUseCase.Request(categoryId = 0L, page = 0)
            val request2 = GetCategoryBoardIdsByOffsetUseCase.Request(categoryId = -1L, page = 0)
            // When & Then
            assertThrows<ConstraintViolationException> { getCategoryBoardIdsByOffset(request1) }
            assertThrows<ConstraintViolationException> { getCategoryBoardIdsByOffset(request2) }
        }
    }
}
