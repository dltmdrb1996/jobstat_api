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

@DisplayName("GetAuthorBoardIdsByCursorUseCase 테스트")
class GetAuthorBoardIdsByCursorUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getAuthorBoardIdsByCursor: GetAuthorBoardIdsByCursorUseCase

    private lateinit var testCategory: BoardCategory
    private val targetAuthor = "cursorAuthor"
    private val otherAuthor = "other"
    private val testLimit = 5

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getAuthorBoardIdsByCursor =
            GetAuthorBoardIdsByCursorUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        val createdBoards = mutableListOf<Long>()
        repeat(12) { createdBoards.add(boardService.createBoard("T $it", "C $it", targetAuthor, testCategory.id, null, (it + 1).toLong()).id) }
        repeat(3) { boardService.createBoard("OT $it", "OC $it", otherAuthor, testCategory.id, null, (it + 100).toLong()) }
        createdBoards.reverse()
        targetBoardIdsDesc = createdBoards
    }

    private lateinit var targetBoardIdsDesc: List<Long>

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
        fun `given author and null cursor, when execute, then return first page ids`() {
            // Given
            val request =
                GetAuthorBoardIdsByCursorUseCase.Request(
                    author = targetAuthor,
                    lastBoardId = null,
                    limit = testLimit,
                )
            val expectedIds = targetBoardIdsDesc.take(testLimit).map { it.toString() }

            // When
            val response = getAuthorBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        fun `given author and cursor from first page, when execute, then return second page ids`() {
            // Given
            val lastIdFromFirstPage = targetBoardIdsDesc[testLimit - 1]
            val request =
                GetAuthorBoardIdsByCursorUseCase.Request(
                    author = targetAuthor,
                    lastBoardId = lastIdFromFirstPage,
                    limit = testLimit,
                )
            val expectedIds = targetBoardIdsDesc.drop(testLimit).take(testLimit).map { it.toString() }

            // When
            val response = getAuthorBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("마지막 페이지 조회")
        fun `given author and cursor from second page, when execute, then return last page ids`() {
            // Given
            val lastIdFromSecondPage = targetBoardIdsDesc[testLimit * 2 - 1]
            val request =
                GetAuthorBoardIdsByCursorUseCase.Request(
                    author = targetAuthor,
                    lastBoardId = lastIdFromSecondPage,
                    limit = testLimit,
                )
            val expectedIds = targetBoardIdsDesc.drop(testLimit * 2).take(testLimit).map { it.toString() }

            // When
            val response = getAuthorBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(2, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("데이터 없는 작성자 조회 시 빈 목록 반환")
        fun `given author with no boards, when execute, then return empty list`() {
            // Given
            val request =
                GetAuthorBoardIdsByCursorUseCase.Request(
                    author = "noBoardsAuthor",
                    lastBoardId = null,
                    limit = testLimit,
                )

            // When
            val response = getAuthorBoardIdsByCursor(request)

            // Then
            assertTrue(response.ids.isEmpty())
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @Test
        @DisplayName("작성자 이름 비어있으면 ConstraintViolationException 발생")
        fun `given blank author, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetAuthorBoardIdsByCursorUseCase.Request(author = " ", lastBoardId = null)
            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByCursor(request) }
        }

        @Test
        @DisplayName("limit 값이 0이거나 음수면 ConstraintViolationException 발생")
        fun `given non-positive limit, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetAuthorBoardIdsByCursorUseCase.Request(author = targetAuthor, lastBoardId = null, limit = 0)
            val request2 = GetAuthorBoardIdsByCursorUseCase.Request(author = targetAuthor, lastBoardId = null, limit = -1)
            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByCursor(request1) }
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByCursor(request2) }
        }

        @Test
        @DisplayName("limit 값이 최대값(100) 초과 시 ConstraintViolationException 발생")
        fun `given limit over max, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetAuthorBoardIdsByCursorUseCase.Request(author = targetAuthor, lastBoardId = null, limit = 101)
            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByCursor(request) }
        }
    }
}
