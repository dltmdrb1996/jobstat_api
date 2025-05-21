package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.BoardFixture
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.statistics_read.core.core_model.BoardRankingMetric
import com.example.jobstat.statistics_read.core.core_model.BoardRankingPeriod
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

@DisplayName("GetRankingBoardIdsByOffsetUseCase 테스트")
class GetRankingBoardIdsByOffsetUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var getRankingBoardIdsByOffset: GetRankingBoardIdsByOffsetUseCase

    private lateinit var testCategory: BoardCategory
    private val defaultPageSize = 3
    private lateinit var boards: Map<String, Board>

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        getRankingBoardIdsByOffset =
            GetRankingBoardIdsByOffsetUseCase(
                boardService = boardService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        setupRankingTestData()
    }

    private fun setupRankingTestData() {
        val now = LocalDateTime.now()
        val boardData = mutableMapOf<String, Board>()
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
        fun `given likes metric and page 0, when execute, then return first page ids by likes`() {
            val request = GetRankingBoardIdsByOffsetUseCase.Request(metric, period, 0, defaultPageSize)
            val expectedIds =
                listOf(
                    boards["B_LikeHigh_ViewMid_Recent"]!!.id,
                    boards["C_LikeMid_ViewLow_RecentTie"]!!.id,
                    boards["B_ViewHigh_LikeMid_Mid"]!!.id,
                ).map { it.toString() }

            val response = getRankingBoardIdsByOffset(request)

            assertEquals(expectedIds, response.ids)
            assertEquals(defaultPageSize, response.ids.size)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        fun `given likes metric and page 1, when execute, then return second page ids by likes`() {
            val request = GetRankingBoardIdsByOffsetUseCase.Request(metric, period, 1, defaultPageSize)
            val expectedIds =
                listOf(
                    boards["E_LikeLow_ViewMid_Mid"]!!.id,
                ).map { it.toString() }

            val response = getRankingBoardIdsByOffset(request)

            assertEquals(expectedIds, response.ids)
            assertEquals(1, response.ids.size)
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @Test
        @DisplayName("페이지 번호 음수 시 ConstraintViolationException 발생")
        fun `given negative page, when execute, then throw ConstraintViolationException`() {
            val request = GetRankingBoardIdsByOffsetUseCase.Request(BoardRankingMetric.LIKES, BoardRankingPeriod.WEEK, -1, defaultPageSize)
            assertThrows<ConstraintViolationException> { getRankingBoardIdsByOffset(request) }
        }

        @Test
        @DisplayName("페이지 크기 0 이하 시 ConstraintViolationException 발생")
        fun `given non-positive size, when execute, then throw ConstraintViolationException`() {
            val request1 = GetRankingBoardIdsByOffsetUseCase.Request(BoardRankingMetric.VIEWS, BoardRankingPeriod.DAY, 0, 0)
            val request2 = GetRankingBoardIdsByOffsetUseCase.Request(BoardRankingMetric.LIKES, BoardRankingPeriod.MONTH, 0, -5)
            assertThrows<ConstraintViolationException> { getRankingBoardIdsByOffset(request1) }
            assertThrows<ConstraintViolationException> { getRankingBoardIdsByOffset(request2) }
        }
    }
}
