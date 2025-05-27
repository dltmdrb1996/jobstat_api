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

@DisplayName("GetLatestBoardIdsByOffsetUseCase 테스트")
class GetLatestBoardIdsByOffsetUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getLatestBoardIdsByOffset: GetLatestBoardIdsByOffsetUseCase

    private lateinit var testCategory: BoardCategory
    private val defaultPageSize = BoardConstants.DEFAULT_PAGE_SIZE // 20
    private lateinit var allBoardIdsDesc: List<Long> // 전체 게시글 ID 목록 (내림차순)

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getLatestBoardIdsByOffset =
            GetLatestBoardIdsByOffsetUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        val createdBoards = mutableListOf<Long>()
        repeat(25) { createdBoards.add(boardService.createBoard("T $it", "C $it", "Author", testCategory.id, null, (it + 1).toLong()).id) }
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
        @DisplayName("첫 페이지 조회 (page=0, size=default)")
        fun `given page 0, when execute, then return first page ids`() {
            // Given
            val request = GetLatestBoardIdsByOffsetUseCase.Request(page = 0, size = defaultPageSize)
            val expectedIds = allBoardIdsDesc.take(defaultPageSize).map { it.toString() }

            // When
            val response = getLatestBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회 (page=1, size=default)")
        fun `given page 1, when execute, then return second page ids`() {
            // Given
            val request = GetLatestBoardIdsByOffsetUseCase.Request(page = 1, size = defaultPageSize)
            val expectedIds = allBoardIdsDesc.drop(defaultPageSize).take(defaultPageSize).map { it.toString() } // 21번째부터 5개

            // When
            val response = getLatestBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(5, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("게시글 없는 경우 빈 목록 반환")
        fun `given no boards, when execute, then return empty list`() {
            // Given
            boardRepository.clear()
            val request = GetLatestBoardIdsByOffsetUseCase.Request(page = 0)

            // When
            val response = getLatestBoardIdsByOffset(request)

            // Then
            assertTrue(response.ids.isEmpty())
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @Test
        @DisplayName("페이지 번호 음수 시 ConstraintViolationException 발생")
        fun `given negative page, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetLatestBoardIdsByOffsetUseCase.Request(page = -1)
            // When & Then
            assertThrows<ConstraintViolationException> { getLatestBoardIdsByOffset(request) }
        }

        @Test
        @DisplayName("페이지 크기 0 이하 시 ConstraintViolationException 발생")
        fun `given non-positive size, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetLatestBoardIdsByOffsetUseCase.Request(page = 0, size = 0)
            val request2 = GetLatestBoardIdsByOffsetUseCase.Request(page = 0, size = -5)
            // When & Then
            assertThrows<ConstraintViolationException> { getLatestBoardIdsByOffset(request1) }
            assertThrows<ConstraintViolationException> { getLatestBoardIdsByOffset(request2) }
        }
    }
}
