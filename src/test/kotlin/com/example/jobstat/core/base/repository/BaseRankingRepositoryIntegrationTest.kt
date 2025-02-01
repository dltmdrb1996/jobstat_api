package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.rankings.document.SkillGrowthRankingsDocument
import com.example.jobstat.rankings.repository.SkillGrowthRankingsRepositoryImpl
import com.example.jobstat.utils.base.BatchOperationTestSupport
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
                totalRecords % batchSize // 마지막 페이지의 실제 크기
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
            if (document.rankings.isNotEmpty()) { // 빈 페이지는 저장하지 않음
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
    fun testFindAllPages() {
        val baseDate = "202401"
        val expectedPages = (totalRecords + batchSize - 1) / batchSize // 올림 처리

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
    fun testFindTopN() {
        val baseDate = "202401"
        val limit = 10

        val result = skillGrowthRankingsRepository.findTopN(baseDate, limit)

        assertEquals(limit, result.size)
        assertEquals((1..limit).toList(), result.map { it.rank })
    }

    @Test
    @Order(4)
    fun testFindByRankRange() {
        val baseDate = "202401"
        val startRank = 95
        val endRank = 105

        val result = skillGrowthRankingsRepository.findByRankRange(baseDate, startRank, endRank)

        // 결과 검증
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
    fun testFindTopMovers() {
        val startDate = "202401"
        val endDate = "202402"
        val limit = 10

        // 추가 테스트 데이터 생성 (다른 날짜의 데이터)
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
        // 랭크 변화가 내림차순으로 정렬되어있는지 확인
        assertEquals(
            result.map { it.rankChange },
            result.map { it.rankChange }.sortedByDescending { it },
        )
    }

    @Test
    @Order(7)
    fun testFindTopLosers() {
        val startDate = "202401"
        val endDate = "202402"
        val limit = 10

        // 테스트 데이터 생성 - 음수 rankChange를 가진 데이터 포함
        val endDateDocument =
            createTestDataForPage(endDate, 1, 1).copy(
                rankings =
                    createTestRankings(1).map {
                        it.copy(rankChange = Random.nextInt(-20, -5)) // 명확한 음수값 설정
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
    fun testFindStableEntities() {
        val months = 3
        val maxRankChange = 5

        // 여러 달의 테스트 데이터 생성
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
    fun testPerformance() {
        startTime = System.currentTimeMillis()

        // 각 작업의 수행 시간 측정
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
            logger.info("Total test execution time: ${duration.inWholeSeconds} seconds")
        }

        // 성능 기준 검증
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
                documentId = "doc_$rank",
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

        // LocalDateTime을 사용하여 날짜 계산
        val startDateTime = LocalDateTime.of(year, month, 1, 0, 0)
        val endDateTime = startDateTime.plusMonths(1).minusSeconds(1)

        // LocalDateTime을 Instant로 변환
        val startDate = startDateTime.toInstant(ZoneOffset.UTC)
        val endDate = endDateTime.toInstant(ZoneOffset.UTC)

        return SnapshotPeriod(startDate, endDate)
    }

    @AfterEach
    fun cleanup() {
        skillGrowthRankingsRepository.deleteAll()
    }
}
