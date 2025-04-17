package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.BoardFixture
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository // saveWithTimestamp 사용
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

@DisplayName("GetRankingBoardIdsByCursorUseCase 테스트")
class GetRankingBoardIdsByCursorUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getRankingBoardIdsByCursor: GetRankingBoardIdsByCursorUseCase

    private lateinit var testCategory: BoardCategory
    private val testLimit = 3
    private lateinit var boards: Map<String, Board>

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getRankingBoardIdsByCursor =
            GetRankingBoardIdsByCursorUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        setupRankingTestData()
    }

    private fun setupRankingTestData() {
        val now = LocalDateTime.now()
        val boardData = mutableMapOf<String, Board>()

        // 데이터 순서 및 값은 예상 랭킹 순서를 고려하여 설정
        boardData["B_LikeHigh_ViewMid_Recent"] =
            boardRepository
                .saveWithTimestamp(
                    BoardFixture
                        .aBoard()
                        .withCategory(testCategory)
                        .withTitle("Like High Recent")
                        .create(),
                    now.minusHours(1),
                ).also {
                    boardRepository.updateLikeCount(it.id, 20)
                    boardRepository.updateViewCount(it.id, 50)
                }

        boardData["B_ViewHigh_LikeMid_Mid"] =
            boardRepository
                .saveWithTimestamp(
                    BoardFixture
                        .aBoard()
                        .withCategory(testCategory)
                        .withTitle("View High Mid")
                        .create(),
                    now.minusDays(3),
                ).also {
                    boardRepository.updateLikeCount(it.id, 10)
                    boardRepository.updateViewCount(it.id, 100)
                }

        boardData["C_LikeMid_ViewLow_RecentTie"] =
            boardRepository
                .saveWithTimestamp(
                    BoardFixture
                        .aBoard()
                        .withCategory(testCategory)
                        .withTitle("Like Mid Recent Tie")
                        .create(),
                    now.minusHours(2),
                ).also {
                    boardRepository.updateLikeCount(it.id, 10)
                    boardRepository.updateViewCount(it.id, 20)
                }

        boardData["D_LikeLow_ViewHigh_Old"] =
            boardRepository
                .saveWithTimestamp(
                    BoardFixture
                        .aBoard()
                        .withCategory(testCategory)
                        .withTitle("Low Like View High Old")
                        .create(),
                    now.minusDays(10),
                ).also {
                    boardRepository.updateLikeCount(it.id, 5)
                    boardRepository.updateViewCount(it.id, 90)
                }

        boardData["E_LikeLow_ViewMid_Mid"] =
            boardRepository
                .saveWithTimestamp(
                    BoardFixture
                        .aBoard()
                        .withCategory(testCategory)
                        .withTitle("Low Like View Mid Mid")
                        .create(),
                    now.minusDays(4),
                ).also {
                    boardRepository.updateLikeCount(it.id, 5)
                    boardRepository.updateViewCount(it.id, 55)
                }

        // ID 역순으로 다시 로드하여 boards 맵에 저장 (ID값 확정)
        boards = boardData.mapValues { boardRepository.findById(it.value.id) }
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("성공 케이스 - LIKES / WEEK")
    inner class LikesWeekSuccessCases {
        private val metric = BoardRankingMetric.LIKES
        private val period = BoardRankingPeriod.WEEK

        @Test
        @DisplayName("첫 페이지 조회")
        fun `given likes metric and null cursor, when execute, then return first page ids by likes`() {
            val request = GetRankingBoardIdsByCursorUseCase.Request(metric, period, null, testLimit)
            val expectedIds =
                listOf(
                    boards["B_LikeHigh_ViewMid_Recent"]!!.id,
                    boards["C_LikeMid_ViewLow_RecentTie"]!!.id,
                    boards["B_ViewHigh_LikeMid_Mid"]!!.id,
                ).map { it.toString() }

            // When
            val response = getRankingBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(testLimit, response.ids.size)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        fun `given likes metric and cursor from first page, when execute, then return second page ids`() {
            val lastBoardIdFromFirstPage = boards["B_ViewHigh_LikeMid_Mid"]!!.id
            val request = GetRankingBoardIdsByCursorUseCase.Request(metric, period, lastBoardIdFromFirstPage, testLimit)
            val expectedIds =
                listOf(
                    boards["E_LikeLow_ViewMid_Mid"]!!.id,
                ).map { it.toString() }

            // When
            val response = getRankingBoardIdsByCursor(request)

            // Then
            assertEquals(expectedIds, response.ids)
            assertEquals(1, response.ids.size)
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("성공 케이스 - VIEWS / DAY")
    inner class ViewsDaySuccessCases {
        private val metric = BoardRankingMetric.VIEWS
        private val period = BoardRankingPeriod.DAY

        @Test
        @DisplayName("DAY 기간 조회 시 최근 게시글만 포함")
        fun `given views metric and day period, when execute, then return only recent boards ranked by views`() {
            val request = GetRankingBoardIdsByCursorUseCase.Request(metric, period, null, testLimit)
            val expectedIds =
                listOf(
                    boards["B_LikeHigh_ViewMid_Recent"]!!.id,
                    boards["C_LikeMid_ViewLow_RecentTie"]!!.id,
                ).map { it.toString() }

            // When
            val response = getRankingBoardIdsByCursor(request)

            assertEquals(expectedIds, response.ids)
            assertEquals(2, response.ids.size)
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
            val request1 = GetRankingBoardIdsByCursorUseCase.Request(BoardRankingMetric.LIKES, BoardRankingPeriod.WEEK, null, 0)
            val request2 = GetRankingBoardIdsByCursorUseCase.Request(BoardRankingMetric.VIEWS, BoardRankingPeriod.DAY, null, -1)
            // When & Then
            assertThrows<ConstraintViolationException> { getRankingBoardIdsByCursor(request1) }
            assertThrows<ConstraintViolationException> { getRankingBoardIdsByCursor(request2) }
        }

        @Test
        @DisplayName("limit 값이 최대값 초과 시 ConstraintViolationException 발생")
        fun `given limit over max, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetRankingBoardIdsByCursorUseCase.Request(BoardRankingMetric.LIKES, BoardRankingPeriod.WEEK, null, 101)
            // When & Then
            assertThrows<ConstraintViolationException> { getRankingBoardIdsByCursor(request) }
        }
    }
}
