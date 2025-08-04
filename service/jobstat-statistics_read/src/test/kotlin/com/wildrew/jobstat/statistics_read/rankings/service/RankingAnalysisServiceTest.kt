package com.wildrew.jobstat.statistics_read.rankings.service

import com.wildrew.jobstat.core.core_global.model.BaseDate
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

    private fun createMockDocument(
        page: Int,
        rankings: List<RankingEntry>,
    ): BaseRankingDocument<RankingEntry> {
        val mockVolatilityMetrics =
            VolatilityMetrics(
                avgRankChange = 2.5,
                rankChangeStdDev = 1.2,
                volatilityTrend = "STABLE",
            )

        val mockMetrics =
            object : RankingMetrics {
                override val totalCount: Int = 200
                override val rankedCount: Int = 200
                override val newEntries: Int = 0
                override val droppedEntries: Int = 0
                override val volatilityMetrics: VolatilityMetrics = mockVolatilityMetrics
            }

        return object : BaseRankingDocument<RankingEntry>(
            id = "mock_id_$page",
            baseDate = "202501",
            period = createSnapshotPeriod(),
            metrics = mockMetrics,
            rankings = rankings,
            page = page,
        ) {
            override fun validate() {
            }
        }
    }

    private fun createMockEntry(
        rank: Int,
        name: String,
        entityId: Long = rank.toLong(),
        rankChange: Int? = null,
        documentId: String = "doc_id",
    ): RankingEntry =
        object : RankingEntry {
            override val documentId = documentId
            override val entityId = entityId
            override val name = name
            override val rank = rank
            override val previousRank = rankChange?.let { rank - it }
            override val rankChange = rankChange
            override val valueChange: Double = 0.0
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
            val baseDate = BaseDate("202501")
            val page = 1
            val mockDoc =
                createMockDocument(
                    page,
                    listOf(
                        createMockEntry(1, "Skill_1"),
                        createMockEntry(2, "Skill_2"),
                    ),
                )
            doReturn(mockDoc).whenever(mockRepository).findByPage(any(), eq(page))

            val result = rankingAnalysisService.findRankingPage(RankingType.SKILL_GROWTH, baseDate, page)

            assertEquals("Skill_1", result.items.data[0].name)
            assertEquals(RankingType.SKILL_GROWTH, result.items.type)
            assertEquals(200, result.rankedCount)
            assertTrue(result.items.data.size >= 2)
            verify(mockRepository).findByPage(baseDate.toString(), page)
        }
    }

    @Test
    @DisplayName("통계와 랭킹 정보를 함께 조회할 수 있다 (커서 기반)")
    fun findStatsWithRankingSuccessfully() {
        val baseDate = BaseDate("202501")
        val cursor = 100
        val limit = 20
        val startRank = 101
        val endRank = 120
        val startPage = 2
        val endPage = 2

        val mockRankingsPage2 = (101..200).map { createMockEntry(it, "Skill_$it", entityId = it.toLong()) }
        val mockDocPage2 = createMockDocument(2, mockRankingsPage2)

        val mockRankingsPage1 = (1..100).map { createMockEntry(it, "Skill_$it", entityId = it.toLong()) }
        val mockDocPage1 = createMockDocument(1, mockRankingsPage1)

        val expectedRankings = mockRankingsPage2.filter { it.rank in startRank..endRank }
        val expectedEntityIds = expectedRankings.map { it.entityId }

        val mockStatsMap =
            expectedEntityIds.associateWith { entityId ->
                mock<BaseStatsDocument>().apply {
                    doReturn(entityId).whenever(this).entityId
                    doReturn(baseDate.toString()).whenever(this).baseDate
                }
            }

        doReturn(listOf(mockDocPage2)).whenever(mockRepository).findByPageRange(baseDate.toString(), startPage, endPage)
        doReturn(mockDocPage1).whenever(mockRepository).findByPage(baseDate.toString(), 1)
        doReturn(mockStatsMap).whenever(mockStatsService).findStatsByEntityIdsAndBaseDate<BaseStatsDocument>(any(), eq(baseDate), eq(expectedEntityIds))

        val result =
            rankingAnalysisService.findStatsWithRanking<BaseStatsDocument>(
                RankingType.SKILL_GROWTH,
                baseDate,
                cursor,
                limit,
            )

        assertNotNull(result)
        assertEquals(limit, result.items.size)
        assertEquals(
            startRank,
            result.items
                .first()
                .ranking.rank,
        )
        assertEquals(
            endRank,
            result.items
                .last()
                .ranking.rank,
        )
        assertEquals(200, result.totalCount)
        assertTrue(result.hasNextPage)
        assertEquals(endRank, result.nextCursor)
        verify(mockRepository).findByPageRange(baseDate.toString(), startPage, endPage)
        verify(mockStatsService).findStatsByEntityIdsAndBaseDate<BaseStatsDocument>(any(), eq(baseDate), eq(expectedEntityIds))
    }
}
