package com.wildrew.jobstat.statistics_read.repository

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.wildrew.jobstat.statistics_read.rankings.document.SkillGrowthRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.repository.SkillGrowthRankingsRepositoryImpl
import com.wildrew.jobstat.statistics_read.utils.config.BatchOperationTestSupport
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.measureTime

@TestMethodOrder(OrderAnnotation::class)
@DisplayName("BaseRankingRepository 통합 테스트")
class BaseRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var skillGrowthRankingsRepository: SkillGrowthRankingsRepositoryImpl

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<SkillGrowthRankingsDocument>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()

    private fun createTestDataForPage(
        baseDate: String,
        page: Int,
        startRank: Int,
    ): SkillGrowthRankingsDocument {
        val actualPageSize =
            if (page * batchSize > totalRecords) {
                totalRecords % batchSize
            } else {
                batchSize
            }

        return SkillGrowthRankingsDocument(
            baseDate = baseDate,
            page = page,
            period = createSnapshotPeriod(baseDate),
            metrics = createTestMetrics(),
            rankings = createTestRankings(startRank).take(actualPageSize),
        )
    }

    @BeforeEach
    override fun setup() {
        val baseDate = "202401"
        val totalPages = (totalRecords + batchSize - 1) / batchSize

        for (page in 1..totalPages) {
            val startRank = (page - 1) * batchSize + 1
            val document = createTestDataForPage(baseDate, page, startRank)
            if (document.rankings.isNotEmpty()) {
                allRecords.add(document)
                skillGrowthRankingsRepository.save(document)
            }
        }
    }

    override fun cleanupTestData() {
        skillGrowthRankingsRepository.deleteAll()
    }

    @Test
    @Order(1)
    @DisplayName("페이지별 랭킹 조회가 가능하다")
    fun testFindByPage() {
        val baseDate = "202401"
        val page = 1

        val result = skillGrowthRankingsRepository.findByPage(baseDate, page)

        assertNotNull(result)
        assertEquals(page, result?.page)
        assertEquals(batchSize, result?.rankings?.size)
        assertEquals(1, result?.rankings?.first()?.rank)
        assertEquals(batchSize, result?.rankings?.last()?.rank)
    }

    @Test
    @Order(2)
    @DisplayName("모든 페이지를 조회할 수 있다")
    fun testFindAllPages() {
        val baseDate = "202401"
        val expectedPages = (totalRecords + batchSize - 1) / batchSize

        runBlocking {
            val results =
                skillGrowthRankingsRepository
                    .findAllPages(baseDate)
                    .toList()

            assertEquals(expectedPages, results.size)
            assertTrue(results.all { it.baseDate == baseDate })
            assertTrue(results.map { it.page }.sorted() == results.map { it.page })
        }
    }

    @Test
    @Order(3)
    @DisplayName("상위 N개의 랭킹을 조회할 수 있다")
    fun testFindTopN() {
        val baseDate = "202401"
        val limit = 10

        val result = skillGrowthRankingsRepository.findTopN(baseDate, limit)

        assertEquals(limit, result.size)
        assertEquals((1..limit).toList(), result.map { it.rank })
    }

    @Test
    @Order(4)
    @DisplayName("특정 순위 범위의 랭킹을 조회할 수 있다")
    fun testFindByRankRange() {
        val baseDate = "202401"
        val startRank = 95
        val endRank = 105

        val result = skillGrowthRankingsRepository.findByRankRange(baseDate, startRank, endRank)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals((endRank - startRank + 1), result.size)
        assertTrue(result.all { it.rank in startRank..endRank })
        assertEquals(
            (startRank..endRank).toList(),
            result.map { it.rank }.sorted(),
        )
    }

    @Test
    @Order(5)
    @DisplayName("엔티티 ID로 랭킹을 조회할 수 있다")
    fun testFindByEntityId() {
        val baseDate = "202401"
        val sampleEntity = allRecords.first().rankings.first()
        val entityId = sampleEntity.entityId

        val result = skillGrowthRankingsRepository.findByEntityId(baseDate, entityId)

        assertNotNull(result)
        assertEquals(entityId, result?.entityId)
        assertEquals(sampleEntity.rank, result?.rank)
    }

    @Test
    @Order(6)
    @DisplayName("상위 상승 엔티티를 조회할 수 있다")
    fun testFindTopMovers() {
        val startDate = "202401"
        val endDate = "202402"
        val limit = 10

        val endDateDocument =
            createTestDataForPage(endDate, 1, 1).copy(
                rankings =
                    createTestRankings(1).map {
                        it.copy(rankChange = Random.nextInt(-20, 21))
                    },
            )
        skillGrowthRankingsRepository.save(endDateDocument)

        val result = skillGrowthRankingsRepository.findTopMovers(startDate, endDate, limit)

        assertEquals(limit, result.size)
        assertEquals(
            result.map { it.rankChange },
            result.map { it.rankChange }.sortedByDescending { it },
        )
    }

    @Test
    @Order(7)
    @DisplayName("상위 하락 엔티티를 조회할 수 있다")
    fun testFindTopLosers() {
        val startDate = "202401"
        val endDate = "202402"
        val limit = 10

        val endDateDocument =
            createTestDataForPage(endDate, 1, 1).copy(
                rankings =
                    createTestRankings(1).map {
                        it.copy(rankChange = Random.nextInt(-20, -5))
                    },
            )
        skillGrowthRankingsRepository.save(endDateDocument)

        val result = skillGrowthRankingsRepository.findTopLosers(startDate, endDate, limit)

        assertEquals(limit, result.size)
        assertEquals(
            result.map { it.rankChange },
            result.map { it.rankChange }.sortedBy { it },
        )
    }

    @Test
    @Order(8)
    @DisplayName("안정적인 엔티티를 조회할 수 있다")
    fun testFindStableEntities() {
        val months = 3
        val maxRankChange = 5

        for (month in 2..months) {
            val baseDate = "2024${month.toString().padStart(2, '0')}"
            val document =
                createTestDataForPage(baseDate, 1, 1).copy(
                    rankings =
                        createTestRankings(1).map {
                            it.copy(rankChange = Random.nextInt(-maxRankChange, maxRankChange + 1))
                        },
                )
            skillGrowthRankingsRepository.save(document)
        }

        val result = skillGrowthRankingsRepository.findStableEntities(months, maxRankChange)

        assertTrue(result.isNotEmpty())
        assertTrue(
            result.all { entry ->
                entry.rankChange == null || abs(entry.rankChange!!) <= maxRankChange
            },
        )
    }

    @Test
    @Order(9)
    @DisplayName("변동성이 큰 엔티티를 조회할 수 있다")
    fun testFindVolatileEntities() {
        val months = 3
        val minRankChange = 10

        val result = skillGrowthRankingsRepository.findVolatileEntities(months, minRankChange)

        assertTrue(
            result.all { entry ->
                entry.rankChange == null || abs(entry.rankChange!!) >= minRankChange
            },
        )
    }

    @Test
    @Order(10)
    @DisplayName("일관된 순위를 유지하는 엔티티를 조회할 수 있다")
    fun testFindEntitiesWithConsistentRanking() {
        val months = 3
        val maxRank = 10

        val result = skillGrowthRankingsRepository.findEntitiesWithConsistentRanking(months, maxRank)

        assertTrue(
            result.all { entry ->
                entry.rank <= maxRank
            },
        )
    }

    @Test
    @Order(12)
    @DisplayName("성능 테스트를 실행한다")
    fun testPerformance() {
        startTime = System.currentTimeMillis()

        measureTime {
            testFindByPage()
            testFindAllPages()
            testFindTopN()
            testFindByRankRange()
            testFindByEntityId()
            testFindTopMovers()
            testFindTopLosers()
            testFindStableEntities()
            testFindVolatileEntities()
            testFindEntitiesWithConsistentRanking()
        }.let { duration ->
            log.debug("Total test execution time: ${duration.inWholeSeconds} seconds")
        }

        performanceMetrics.forEach { (operation, time) ->
            assertTrue(
                time <
                    when (operation) {
                        "find_by_page" -> 1.0
                        "find_all_pages" -> 2.0
                        "find_top_n" -> 1.0
                        "find_by_rank_range" -> 1.0
                        "find_by_entity_id" -> 1.0
                        "find_top_movers" -> 2.0
                        "find_top_losers" -> 2.0
                        "find_stable_entities" -> 3.0
                        "find_volatile_entities" -> 3.0
                        "find_consistent_ranking" -> 3.0
                        "find_ranking_history" -> 2.0
                        else -> Double.MAX_VALUE
                    },
                "$operation took too long: $time seconds",
            )
        }
    }

    private fun createTestMetrics(): SkillGrowthRankingsDocument.SkillGrowthMetrics =
        SkillGrowthRankingsDocument.SkillGrowthMetrics(
            totalCount = totalRecords,
            rankedCount = totalRecords,
            newEntries = 0,
            droppedEntries = 0,
            volatilityMetrics =
                VolatilityMetrics(
                    avgRankChange = Random.nextDouble(0.0, 5.0),
                    rankChangeStdDev = Random.nextDouble(0.0, 2.0),
                    volatilityTrend = "STABLE",
                ),
            growthAnalysis =
                SkillGrowthRankingsDocument.SkillGrowthMetrics.GrowthAnalysis(
                    avgGrowthRate = Random.nextDouble(-20.0, 50.0),
                    medianGrowthRate = Random.nextDouble(-20.0, 50.0),
                    growthDistribution =
                        mapOf(
                            "high_growth" to Random.nextInt(100),
                            "medium_growth" to Random.nextInt(100),
                            "low_growth" to Random.nextInt(100),
                        ),
                ),
        )

    private fun createTestRankings(startRank: Int): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> =
        (0 until batchSize).map { index ->
            val rank = startRank + index
            SkillGrowthRankingsDocument.SkillGrowthRankingEntry(
                entityId = rank.toLong(),
                name = "Skill_$rank",
                rank = rank,
                previousRank = rank + Random.nextInt(-5, 6),
                rankChange = Random.nextInt(-10, 11),
                score = Random.nextDouble(0.0, 100.0),
                growthRate = Random.nextDouble(-20.0, 50.0),
                growthConsistency = Random.nextDouble(0.0, 1.0),
                growthFactors =
                    SkillGrowthRankingsDocument.SkillGrowthRankingEntry.GrowthFactors(
                        demandGrowth = Random.nextDouble(0.0, 30.0),
                        salaryGrowth = Random.nextDouble(0.0, 20.0),
                        adoptionRate = Random.nextDouble(0.0, 1.0),
                        marketPenetration = Random.nextDouble(0.0, 1.0),
                    ),
            )
        }

    private fun createSnapshotPeriod(baseDate: String): SnapshotPeriod {
        val year = baseDate.substring(0, 4).toInt()
        val month = baseDate.substring(4, 6).toInt()

        val startDateTime = LocalDateTime.of(year, month, 1, 0, 0)
        val endDateTime = startDateTime.plusMonths(1).minusSeconds(1)

        val startDate = startDateTime.toInstant(ZoneOffset.UTC)
        val endDate = endDateTime.toInstant(ZoneOffset.UTC)

        return SnapshotPeriod(startDate, endDate)
    }

    @AfterEach
    fun cleanup() {
        skillGrowthRankingsRepository.deleteAll()
    }
}
