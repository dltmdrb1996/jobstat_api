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
import org.mockito.kotlin.*

@DisplayName("GetAuthorBoardIdsByOffsetUseCase 테스트")
class GetAuthorBoardIdsByOffsetUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getAuthorBoardIdsByOffset: GetAuthorBoardIdsByOffsetUseCase

    private lateinit var testCategory: BoardCategory
    private val targetAuthor = "offsetAuthor"
    private val otherAuthor = "other"
    private val defaultPageSize = BoardConstants.DEFAULT_PAGE_SIZE // 예시: 20

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getAuthorBoardIdsByOffset =
            GetAuthorBoardIdsByOffsetUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        val createdBoards = mutableListOf<Long>()
        repeat(25) { createdBoards.add(boardService.createBoard("T $it", "C $it", targetAuthor, testCategory.id, null, (it + 1).toLong()).id) }
        repeat(5) { boardService.createBoard("OT $it", "OC $it", otherAuthor, testCategory.id, null, (it + 100).toLong()) }
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
        @DisplayName("첫 페이지 조회 (page=0, size=default)")
        fun `given author and page 0, when execute, then return first page ids`() {
            // Given
            val request = GetAuthorBoardIdsByOffsetUseCase.Request(author = targetAuthor, page = 0, size = defaultPageSize)
            val expectedIds = targetBoardIdsDesc.take(defaultPageSize).map { it.toString() }

            // When
            val response = getAuthorBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회 (page=1, size=default)")
        fun `given author and page 1, when execute, then return second page ids`() {
            // Given
            val request = GetAuthorBoardIdsByOffsetUseCase.Request(author = targetAuthor, page = 1, size = defaultPageSize)
            val expectedIds = targetBoardIdsDesc.drop(defaultPageSize).take(defaultPageSize).map { it.toString() } // 21번째부터 (5개 남음)

            // When
            val response = getAuthorBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(5, response.ids.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("특정 페이지 크기로 조회")
        fun `given author and specific size, when execute, then return correct number of ids`() {
            // Given
            val customSize = 7
            val request = GetAuthorBoardIdsByOffsetUseCase.Request(author = targetAuthor, page = 0, size = customSize)
            val expectedIds = targetBoardIdsDesc.take(customSize).map { it.toString() }

            // When
            val response = getAuthorBoardIdsByOffset(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(customSize, response.ids.size)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("데이터 없는 작성자 조회 시 빈 목록 반환")
        fun `given author with no boards, when execute, then return empty list`() {
            // Given
            val request = GetAuthorBoardIdsByOffsetUseCase.Request(author = "noBoardsAuthor", page = 0)

            // When
            val response = getAuthorBoardIdsByOffset(request)

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
            val request = GetAuthorBoardIdsByOffsetUseCase.Request(author = " ", page = 0)
            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByOffset(request) }
        }

        @Test
        @DisplayName("페이지 번호 음수면 ConstraintViolationException 발생")
        fun `given negative page, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetAuthorBoardIdsByOffsetUseCase.Request(author = targetAuthor, page = -1)
            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByOffset(request) }
        }

        @Test
        @DisplayName("페이지 크기 0 이하면 ConstraintViolationException 발생")
        fun `given non-positive size, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetAuthorBoardIdsByOffsetUseCase.Request(author = targetAuthor, page = 0, size = 0)
            val request2 = GetAuthorBoardIdsByOffsetUseCase.Request(author = targetAuthor, page = 0, size = -5)
            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByOffset(request1) }
            assertThrows<ConstraintViolationException> { getAuthorBoardIdsByOffset(request2) }
        }
    }
}
