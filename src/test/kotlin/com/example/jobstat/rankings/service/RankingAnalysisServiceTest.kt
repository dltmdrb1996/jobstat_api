package com.example.jobstat.rankings.service

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingType
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.base.repository.BaseRankingRepository
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.rankings.document.SkillGrowthRankingsDocument
import com.example.jobstat.rankings.repository.RankingRepositoryRegistry
import com.example.jobstat.rankings.repository.SkillGrowthRankingsRepository
import com.example.jobstat.utils.FakeStringRedisTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("RankingAnalysisService 테스트")
class RankingAnalysisServiceTest {
    private lateinit var mockRepository: SkillGrowthRankingsRepository
    private lateinit var repositoryRegistry: RankingRepositoryRegistry
    private lateinit var redisTemplate: FakeStringRedisTemplate
    private lateinit var rankingAnalysisService: RankingAnalysisService
    private val random = Random(12345)

    @BeforeEach
    fun setUp() {
        mockRepository = mock()
        redisTemplate = FakeStringRedisTemplate()

        val repositories = listOf<BaseRankingRepository<*, *, *>>(mockRepository)
        repositoryRegistry = RankingRepositoryRegistry(repositories)

        rankingAnalysisService =
            RankingAnalysisServiceImpl(
                repositoryRegistry = repositoryRegistry,
                redisTemplate = redisTemplate,
            )
    }

    private fun createSnapshotPeriod(baseDate: String): SnapshotPeriod {
        val year = baseDate.substring(0, 4).toInt()
        val month = baseDate.substring(4, 6).toInt()

        val startDate = Instant.parse("$year-${month.toString().padStart(2, '0')}-01T00:00:00Z")
        val nextMonth =
            if (month == 12) {
                "${year + 1}-01-01T00:00:00Z"
            } else {
                "$year-${(month + 1).toString().padStart(2, '0')}-01T00:00:00Z"
            }
        val endDate = Instant.parse(nextMonth).minusSeconds(1)

        return SnapshotPeriod(startDate, endDate)
    }

    private fun createSkillDocument(
        baseDate: String,
        rank: Int,
        entityId: Long,
        previousRank: Int? = null,
        rankChange: Int? = null,
    ): SkillGrowthRankingsDocument {
        val growthRate = random.nextDouble(-20.0, 50.0)
        val consistency = random.nextDouble(0.0, 1.0)
        val actualPreviousRank = previousRank ?: rank + random.nextInt(-5, 5)
        val actualRankChange = rankChange ?: (actualPreviousRank - rank)

        return SkillGrowthRankingsDocument(
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            metrics =
                SkillGrowthRankingsDocument.SkillGrowthMetrics(
                    totalCount = 100,
                    rankedCount = 100,
                    newEntries = 0,
                    droppedEntries = 0,
                    volatilityMetrics =
                        VolatilityMetrics(
                            avgRankChange = random.nextDouble(0.0, 5.0),
                            rankChangeStdDev = random.nextDouble(0.0, 2.0),
                            volatilityTrend = "STABLE",
                        ),
                    growthAnalysis =
                        SkillGrowthRankingsDocument.SkillGrowthMetrics.GrowthAnalysis(
                            avgGrowthRate = growthRate,
                            medianGrowthRate = growthRate + random.nextDouble(-2.0, 2.0),
                            growthDistribution =
                                mapOf(
                                    "high_growth" to random.nextInt(100),
                                    "medium_growth" to random.nextInt(100),
                                    "low_growth" to random.nextInt(100),
                                ),
                        ),
                ),
            rankings =
                listOf(
                    SkillGrowthRankingsDocument.SkillGrowthRankingEntry(
                        documentId = "SkillGrowthRankingEntry_$entityId",
                        entityId = entityId,
                        name = "Skill_$entityId",
                        rank = rank,
                        previousRank = actualPreviousRank,
                        rankChange = actualRankChange,
                        score = random.nextDouble(0.0, 100.0),
                        growthRate = growthRate,
                        growthConsistency = consistency,
                        growthFactors =
                            SkillGrowthRankingsDocument.SkillGrowthRankingEntry.GrowthFactors(
                                demandGrowth = random.nextDouble(0.0, 30.0),
                                salaryGrowth = random.nextDouble(0.0, 20.0),
                                adoptionRate = random.nextDouble(0.0, 1.0),
                                marketPenetration = random.nextDouble(0.0, 1.0),
                            ),
                    ),
                ),
        )
    }

    @Nested
    @DisplayName("랭킹 페이지 조회")
    inner class FindRankingPage {
        @Test
        @DisplayName("정상적으로 랭킹 페이지를 조회할 수 있다")
        fun findRankingPageSuccessfully() {
            // given
            val baseDate = BaseDate("202501")
            val page = 1
            val document = createSkillDocument("202501", 1, 1L)

            doReturn(document)
                .`when`(mockRepository)
                .findByPage(any(), eq(page))

            // when
            val rankingPage =
                rankingAnalysisService.findRankingPage(
                    RankingType.SKILL_GROWTH,
                    baseDate,
                    page,
                )

            // then
            assertNotNull(rankingPage)
            assertTrue(rankingPage.items.data.isNotEmpty())
            assertEquals("Skill_1", rankingPage.items.data[0].name)
            assertEquals(RankingType.SKILL_GROWTH, rankingPage.items.type)

            verify(mockRepository).findByPage(eq(baseDate.toString()), eq(page))
        }

        @Test
        @DisplayName("페이지 번호가 null일 경우 첫 페이지를 조회한다")
        fun findFirstPageWhenPageIsNull() {
            // given
            val baseDate = BaseDate("202501")
            val document = createSkillDocument("202501", 1, 1L)

            doReturn(document)
                .`when`(mockRepository)
                .findByPage(any(), eq(1))

            // when
            val rankingPage =
                rankingAnalysisService.findRankingPage(
                    RankingType.SKILL_GROWTH,
                    baseDate,
                    null,
                )

            // then
            assertNotNull(rankingPage)
            verify(mockRepository).findByPage(eq(baseDate.toString()), eq(1))
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
            val rankings =
                (1..5).map { rank ->
                    createSkillDocument("202501", rank, rank.toLong()).rankings.first()
                }

            doReturn(rankings)
                .`when`(mockRepository)
                .findTopN(any(), any())

            // when
            val result =
                rankingAnalysisService.findTopNRankings(
                    RankingType.SKILL_GROWTH,
                    baseDate,
                    limit,
                )

            // then
            assertNotNull(result)
            assertEquals(5, result.size)
            assertTrue(result.all { it.rank <= limit })

            verify(mockRepository).findTopN(eq(baseDate.toString()), eq(limit))
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
            val rankings =
                listOf(
                    createSkillDocument("202501", 1, 1L, previousRank = 5).rankings.first(),
                    createSkillDocument("202501", 2, 2L, previousRank = 7).rankings.first(),
                )

            doReturn(rankings)
                .`when`(mockRepository)
                .findTopMovers(any(), any(), any())

            // when
            val result =
                rankingAnalysisService.findRankingMovements(
                    RankingType.SKILL_GROWTH,
                    startDate,
                    endDate,
                    limit,
                )

            // then
            assertNotNull(result)
            assertEquals(2, result.size)
            assertTrue(result[0].rankChange!! > 0)

            verify(mockRepository).findTopMovers(
                eq(startDate.toString()),
                eq(endDate.toString()),
                eq(limit),
            )
        }
    }

    @Nested
    @DisplayName("일관된 랭킹 조회")
    inner class FindConsistentRankings {
        @Test
        @DisplayName("일정 기간 동안 일관된 순위를 유지한 항목들을 조회할 수 있다")
        fun findConsistentRankingsSuccessfully() {
            // given
            val months = 3
            val maxRank = 10
            val rankings =
                listOf(
                    createSkillDocument("202501", 1, 1L).rankings.first(),
                    createSkillDocument("202501", 2, 2L).rankings.first(),
                )

            doReturn(rankings)
                .`when`(mockRepository)
                .findEntitiesWithConsistentRanking(any(), any())

            // when
            val result =
                rankingAnalysisService.findConsistentRankings(
                    RankingType.SKILL_GROWTH,
                    months,
                    maxRank,
                )

            // then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.all { it.rank <= maxRank })

            verify(mockRepository).findEntitiesWithConsistentRanking(eq(months), eq(maxRank))
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
            val rankings =
                (startRank..endRank).map { rank ->
                    createSkillDocument("202501", rank, rank.toLong()).rankings.first()
                }

            doReturn(rankings)
                .`when`(mockRepository)
                .findByRankRange(any(), any(), any())

            // when
            val result =
                rankingAnalysisService.findRankRange(
                    RankingType.SKILL_GROWTH,
                    baseDate,
                    startRank,
                    endRank,
                )

            // then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.all { it.rank in startRank..endRank })

            verify(mockRepository).findByRankRange(eq(baseDate.toString()), eq(startRank), eq(endRank))
        }
    }

    @Nested
    @DisplayName("변동성 높은 랭킹 조회")
    inner class FindVolatileRankings {
        @Test
        @DisplayName("변동성이 높은 랭킹들을 조회할 수 있다")
        fun findVolatileRankingsSuccessfully() {
            // given
            val months = 3
            val minRankChange = 10
            val rankings =
                listOf(
                    createSkillDocument("202501", 15, 1L, previousRank = 1).rankings.first(),
                    createSkillDocument("202501", 1, 2L, previousRank = 20).rankings.first(),
                )

            doReturn(rankings)
                .`when`(mockRepository)
                .findVolatileEntities(any(), any())

            // when
            val result =
                rankingAnalysisService.findVolatileRankings(
                    RankingType.SKILL_GROWTH,
                    months,
                    minRankChange,
                )

            // then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(
                result.all {
                    it.rankChange?.let { change -> abs(change) >= minRankChange } ?: false
                },
            )

            verify(mockRepository).findVolatileEntities(eq(months), eq(minRankChange))
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
            val rankings =
                listOf(
                    createSkillDocument("202501", 15, 1L, previousRank = 5).rankings.first(),
                    createSkillDocument("202501", 20, 2L, previousRank = 8).rankings.first(),
                )

            doReturn(rankings)
                .`when`(mockRepository)
                .findTopLosers(any(), any(), any())

            // when
            val result =
                rankingAnalysisService.findTopLosers(
                    RankingType.SKILL_GROWTH,
                    startDate,
                    endDate,
                    limit,
                )

            // then
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertTrue(result.all { it.rankChange!! < 0 })

            verify(mockRepository).findTopLosers(
                eq(startDate.toString()),
                eq(endDate.toString()),
                eq(limit),
            )
        }
    }
}
