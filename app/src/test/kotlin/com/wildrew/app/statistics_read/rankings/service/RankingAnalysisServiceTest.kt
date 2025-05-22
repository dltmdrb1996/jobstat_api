package com.wildrew.app.statistics_read.rankings.service

import com.wildrew.jobstat.statistics_read.core.core_model.BaseDate
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseRankingRepository
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.rankings.repository.RankingRepositoryRegistry
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("RankingAnalysisService 테스트")
class RankingAnalysisServiceTest {
    private lateinit var mockRepository: BaseRankingRepository<BaseRankingDocument<RankingEntry>, RankingEntry, String>
    private lateinit var mockStatsService: StatsAnalysisService
    private lateinit var mockRepositoryRegistry: RankingRepositoryRegistry
    private lateinit var rankingAnalysisService: RankingAnalysisServiceImpl

    @BeforeEach
    fun setUp() {
        mockRepository = mock()
        mockStatsService = mock()
        mockRepositoryRegistry = mock()

        doReturn(mockRepository).whenever(mockRepositoryRegistry).getRepository<BaseRankingDocument<*>>(any())

        rankingAnalysisService =
            RankingAnalysisServiceImpl(
                repositoryRegistry = mockRepositoryRegistry,
                statsService = mockStatsService,
            )
    }

    private fun createMockDocument(rankings: List<RankingEntry>): BaseRankingDocument<RankingEntry> {
        val mockVolatilityMetrics =
            VolatilityMetrics(
                avgRankChange = 2.5,
                rankChangeStdDev = 1.2,
                volatilityTrend = "STABLE",
            )

        val mockMetrics =
            object : RankingMetrics {
                override val totalCount: Int = 100
                override val rankedCount: Int = 100
                override val newEntries: Int = 0
                override val droppedEntries: Int = 0
                override val volatilityMetrics: VolatilityMetrics = mockVolatilityMetrics
            }

        return object : BaseRankingDocument<RankingEntry>(
            id = "mock_id",
            baseDate = "202501",
            period = createSnapshotPeriod(),
            metrics = mockMetrics,
            rankings = rankings,
            page = 1,
        ) {
            override fun validate() {
                // 테스트용 mock이므로 validation 생략
            }
        }
    }

    private fun createMockEntry(
        rank: Int,
        name: String,
        entityId: Long = rank.toLong(),
        rankChange: Int? = null,
    ): RankingEntry =
        object : RankingEntry {
            override val entityId = entityId
            override val name = name
            override val rank = rank
            override val previousRank = rankChange?.let { rank - it }
            override val rankChange = rankChange
        }

    private fun createSnapshotPeriod(): SnapshotPeriod =
        SnapshotPeriod(
            startDate = Instant.parse("2025-01-01T00:00:00Z"),
            endDate = Instant.parse("2025-01-31T23:59:59Z"),
        )

    @Nested
    @DisplayName("랭킹 페이지 조회")
    inner class FindRankingPage {
        @Test
        @DisplayName("정상적으로 랭킹 페이지를 조회할 수 있다")
        fun findRankingPageSuccessfully() {
            // given
            val baseDate = BaseDate("202501")
            val page = 1
            val mockDoc =
                createMockDocument(
                    listOf(
                        createMockEntry(1, "Skill_1"),
                        createMockEntry(2, "Skill_2"),
                    ),
                )
            doReturn(mockDoc).whenever(mockRepository).findByPage(any(), eq(page))

            // when
            val result = rankingAnalysisService.findRankingPage(RankingType.SKILL_GROWTH, baseDate, page)

            // then
            assertEquals("Skill_1", result.items.data[0].name)
            assertEquals(RankingType.SKILL_GROWTH, result.items.type)
            assertEquals(100, result.rankedCount)
            assertTrue(result.items.data.size >= 2)
            verify(mockRepository).findByPage(baseDate.toString(), page)
        }

        @Test
        @DisplayName("페이지 번호가 null일 경우 첫 페이지를 조회한다")
        fun findFirstPageWhenPageIsNull() {
            // given
            val baseDate = BaseDate("202501")
            val mockDoc = createMockDocument(listOf(createMockEntry(1, "Skill_1")))
            doReturn(mockDoc).whenever(mockRepository).findByPage(any(), eq(1))

            // when
            val result = rankingAnalysisService.findRankingPage(RankingType.SKILL_GROWTH, baseDate, null)

            // then
            assertEquals(1, result.items.data[0].rank)
            assertEquals(100, result.rankedCount)
            verify(mockRepository).findByPage(baseDate.toString(), 1)
        }
    }

    @Nested
    @DisplayName("상위 N개 랭킹 조회")
    inner class FindTopNRankings {
        @Test
        @DisplayName("상위 N개의 랭킹을 조회할 수 있다")
        fun findTopNRankingsSuccessfully() {
            // given
            val baseDate = BaseDate("202501")
            val limit = 5
            val mockEntries = (1..5).map { createMockEntry(it, "Skill_$it") }
            doReturn(mockEntries).whenever(mockRepository).findTopN(any(), eq(limit))

            // when
            val result = rankingAnalysisService.findTopNRankings(RankingType.SKILL_GROWTH, baseDate, limit)

            // then
            assertEquals(limit, result.size)
            assertTrue(result.all { it.rank <= limit })
            verify(mockRepository).findTopN(baseDate.toString(), limit)
        }
    }

    @Nested
    @DisplayName("랭킹 변동 조회")
    inner class FindRankingMovements {
        @Test
        @DisplayName("랭킹 상승이 큰 항목들을 조회할 수 있다")
        fun findTopMoversSuccessfully() {
            // given
            val startDate = BaseDate("202501")
            val endDate = BaseDate("202502")
            val limit = 2
            val mockEntries =
                listOf(
                    createMockEntry(1, "Skill_1", rankChange = 5),
                    createMockEntry(2, "Skill_2", rankChange = 3),
                )
            doReturn(mockEntries).whenever(mockRepository).findTopMovers(any(), any(), eq(limit))

            // when
            val result = rankingAnalysisService.findRankingMovements(RankingType.SKILL_GROWTH, startDate, endDate, limit)

            // then
            assertEquals(limit, result.size)
            assertTrue(result.all { it.rankChange != null && (it.rankChange ?: 0) > 0 })
            verify(mockRepository).findTopMovers(startDate.toString(), endDate.toString(), limit)
        }
    }

    @Nested
    @DisplayName("일관된 랭킹 조회")
    inner class FindConsistentRankings {
        @Test
        @DisplayName("일정 기간 동안 일관된 순위를 유지한 항목들을 조회할 수 있다")
        fun findConsistentRankingsSuccessfully() {
            // given
            val months = 2
            val maxRank = 10
            val mockEntries =
                listOf(
                    createMockEntry(1, "Skill_1", rankChange = 0),
                    createMockEntry(2, "Skill_2", rankChange = 0),
                )
            doReturn(mockEntries).whenever(mockRepository).findEntitiesWithConsistentRanking(eq(months), eq(maxRank))

            // when
            val result = rankingAnalysisService.findConsistentRankings(RankingType.SKILL_GROWTH, months, maxRank)

            // then
            assertTrue(result.all { it.rank <= maxRank })
            verify(mockRepository).findEntitiesWithConsistentRanking(months, maxRank)
        }
    }

    @Nested
    @DisplayName("랭킹 범위 조회")
    inner class FindRankRange {
        @Test
        @DisplayName("특정 순위 범위의 항목들을 조회할 수 있다")
        fun findRankRangeSuccessfully() {
            // given
            val baseDate = BaseDate("202501")
            val startRank = 5
            val endRank = 10
            val mockEntries =
                (startRank..endRank).map {
                    createMockEntry(it, "Skill_$it")
                }
            doReturn(mockEntries).whenever(mockRepository).findByRankRange(any(), eq(startRank), eq(endRank))

            // when
            val result = rankingAnalysisService.findRankRange(RankingType.SKILL_GROWTH, baseDate, startRank, endRank)

            // then
            assertTrue(result.all { it.rank in startRank..endRank })
            assertEquals(endRank - startRank + 1, result.size)
            verify(mockRepository).findByRankRange(baseDate.toString(), startRank, endRank)
        }
    }

    @Nested
    @DisplayName("변동성 높은 랭킹 조회")
    inner class FindVolatileRankings {
        @Test
        @DisplayName("변동성이 높은 랭킹들을 조회할 수 있다")
        fun findVolatileRankingsSuccessfully() {
            // given
            val months = 2
            val minRankChange = 10
            val mockEntries =
                listOf(
                    createMockEntry(15, "Skill_1", rankChange = -14),
                    createMockEntry(1, "Skill_2", rankChange = 19),
                )
            doReturn(mockEntries).whenever(mockRepository).findVolatileEntities(eq(months), eq(minRankChange))

            // when
            val result = rankingAnalysisService.findVolatileRankings(RankingType.SKILL_GROWTH, months, minRankChange)

            // then
            assertTrue(
                result.all { entry ->
                    entry.rankChange?.let { change -> kotlin.math.abs(change) >= minRankChange } ?: false
                },
            )
            verify(mockRepository).findVolatileEntities(months, minRankChange)
        }
    }

    @Nested
    @DisplayName("순위 하락자 조회")
    inner class FindTopLosers {
        @Test
        @DisplayName("순위가 가장 많이 하락한 항목들을 조회할 수 있다")
        fun findTopLosersSuccessfully() {
            // given
            val startDate = BaseDate("202501")
            val endDate = BaseDate("202502")
            val limit = 2
            val mockEntries =
                listOf(
                    createMockEntry(15, "Skill_1", rankChange = -10),
                    createMockEntry(20, "Skill_2", rankChange = -12),
                )
            doReturn(mockEntries).whenever(mockRepository).findTopLosers(any(), any(), eq(limit))

            // when
            val result = rankingAnalysisService.findTopLosers(RankingType.SKILL_GROWTH, startDate, endDate, limit)

            // then
            assertEquals(limit, result.size)
            assertTrue(result.all { it.rankChange!! < 0 })
            verify(mockRepository).findTopLosers(startDate.toString(), endDate.toString(), limit)
        }
    }

    @Test
    @DisplayName("통계와 랭킹 정보를 함께 조회할 수 있다")
    fun findStatsWithRankingSuccessfully() {
        // given
        val baseDate = BaseDate("202501")
        val page = 1
        val mockRankings =
            listOf(
                createMockEntry(1, "Skill_1", entityId = 101),
                createMockEntry(2, "Skill_2", entityId = 102),
            )
        val mockDoc = createMockDocument(mockRankings)

        // Map 형태로 반환할 통계 데이터 생성
        val mockStatsMap =
            mockRankings.associate { ranking ->
                val mockStat =
                    mock<BaseStatsDocument>().apply {
                        doReturn(ranking.entityId).whenever(this).entityId
                        doReturn(baseDate.toString()).whenever(this).baseDate
                    }
                ranking.entityId to mockStat
            }

        doReturn(mockDoc).whenever(mockRepository).findByPage(any(), eq(page))

        // findStatsByEntityIdsAndBaseDate 메소드 모킹 (Map 반환)
        doReturn(mockStatsMap)
            .whenever(mockStatsService)
            .findStatsByEntityIdsAndBaseDate<BaseStatsDocument>(
                any(),
                eq(baseDate),
                eq(mockRankings.map { it.entityId }),
            )

        // when
        val result =
            rankingAnalysisService.findStatsWithRanking<BaseStatsDocument>(
                RankingType.SKILL_GROWTH,
                baseDate,
                page,
            )

        // then
        assertNotNull(result)
        assertEquals(mockRankings.size, result.items.size)
        verify(mockRepository).findByPage(baseDate.toString(), page)
        verify(mockStatsService).findStatsByEntityIdsAndBaseDate<BaseStatsDocument>(
            any(),
            eq(baseDate),
            any(),
        )
    }

    @Nested
    @DisplayName("예외 상황 처리")
    inner class ExceptionHandling {
        @Test
        @DisplayName("존재하지 않는 날짜의 데이터 요청 시 예외가 발생한다")
        fun handleNonExistentDate() {
            // given
            val nonExistentDate = BaseDate("202401")
            doThrow(NoSuchElementException()).whenever(mockRepository).findByPage(eq(nonExistentDate.toString()), any())

            // when & then
            assertFailsWith<NoSuchElementException> {
                rankingAnalysisService.findRankingPage(RankingType.SKILL_GROWTH, nonExistentDate, 1)
            }
        }

        @Test
        @DisplayName("잘못된 랭킹 유형 요청 시 예외가 발생한다")
        fun handleInvalidRankingType() {
            // given
            doThrow(IllegalArgumentException()).whenever(mockRepositoryRegistry).getRepository<BaseRankingDocument<*>>(
                eq(
                    RankingType.COMPANY_GROWTH,
                ),
            )

            // when & then
            assertFailsWith<IllegalArgumentException> {
                rankingAnalysisService.findRankingPage(RankingType.COMPANY_GROWTH, BaseDate("202501"), 1)
            }
        }
    }
}
