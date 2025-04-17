package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("GetLatestBoardIdsByCursorUseCase 테스트")
class GetLatestBoardIdsByCursorUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getLatestBoardIdsByCursor: GetLatestBoardIdsByCursorUseCase

    private lateinit var testCategory: BoardCategory
    private val testLimit = 5
    private lateinit var allBoardIdsDesc: List<Long>

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getLatestBoardIdsByCursor =
            GetLatestBoardIdsByCursorUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        val createdBoards = mutableListOf<Long>()
        repeat(12) { createdBoards.add(boardService.createBoard("T $it", "C $it", "Author", testCategory.id, null, (it + 1).toLong()).id) }
        createdBoards.reverse()
        allBoardIdsDesc = createdBoards
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
        @DisplayName("첫 페이지 조회 (lastBoardId = null)")
        fun `given null cursor, when execute, then return first page ids`() {
            // Given
            val request = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = null, limit = testLimit)
            val expectedIds = allBoardIdsDesc.take(testLimit).map { it.toString() }

            // When
            val response = getLatestBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        fun `given cursor from first page, when execute, then return second page ids`() {
            // Given
            val lastIdFromFirstPage = allBoardIdsDesc[testLimit - 1]
            val request = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = lastIdFromFirstPage, limit = testLimit)
            val expectedIds = allBoardIdsDesc.drop(testLimit).take(testLimit).map { it.toString() }

            // When
            val response = getLatestBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("마지막 페이지 조회")
        fun `given cursor from second page, when execute, then return last page ids`() {
            // Given
            val lastIdFromSecondPage = allBoardIdsDesc[testLimit * 2 - 1]
            val request = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = lastIdFromSecondPage, limit = testLimit)
            val expectedIds = allBoardIdsDesc.drop(testLimit * 2).take(testLimit).map { it.toString() } // 2개 남음

            // When
            val response = getLatestBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(2, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("게시글 없는 경우 빈 목록 반환")
        fun `given no boards, when execute, then return empty list`() {
            // Given
            boardRepository.clear()
            val request = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = null, limit = testLimit)

            // When
            val response = getLatestBoardIdsByCursor(request)

            // Then
            assertTrue(response.ids.isEmpty())
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @Test
        @DisplayName("limit 값이 0 이하면 ConstraintViolationException 발생")
        fun `given non-positive limit, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = null, limit = 0)
            val request2 = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = null, limit = -1)
            // When & Then
            assertThrows<ConstraintViolationException> { getLatestBoardIdsByCursor(request1) }
            assertThrows<ConstraintViolationException> { getLatestBoardIdsByCursor(request2) }
        }

        @Test
        @DisplayName("limit 값이 최대값(100) 초과 시 ConstraintViolationException 발생")
        fun `given limit over max, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetLatestBoardIdsByCursorUseCase.Request(lastBoardId = null, limit = 101)
            // When & Then
            assertThrows<ConstraintViolationException> { getLatestBoardIdsByCursor(request) }
        }
    }
}
