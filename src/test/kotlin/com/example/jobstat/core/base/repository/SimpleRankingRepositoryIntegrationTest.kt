package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.rankings.model.SkillGrowthRankingsDocument
import com.example.jobstat.rankings.repository.SkillGrowthRankingsRepositoryImpl
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SimpleRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var skillGrowthRankingsRepository: SkillGrowthRankingsRepositoryImpl

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<SkillGrowthRankingsDocument>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()

    override fun cleanupTestData() {
        val recordIds = allRecords.mapNotNull { it.id }
        for (batch in recordIds.chunked(batchSize)) {
            skillGrowthRankingsRepository.bulkDelete(batch)
        }
    }

    @BeforeAll
    fun beforeAll() {
    }

    @AfterAll
    fun afterAll() {
        cleanupTestData()
        printExecutionSummary()
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

    private fun createSkillGrowthDocument(
        baseDate: String,
        rank: Int,
        entityId: Long,
        growthRate: Double,
    ): SkillGrowthRankingsDocument =
        SkillGrowthRankingsDocument(
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            metrics =
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
                            avgGrowthRate = growthRate,
                            medianGrowthRate = growthRate + Random.nextDouble(-2.0, 2.0),
                            growthDistribution =
                                mapOf(
                                    "high_growth" to Random.nextInt(100),
                                    "medium_growth" to Random.nextInt(100),
                                    "low_growth" to Random.nextInt(100),
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
                        previousRank = rank + Random.nextInt(-5, 5),
                        rankChange = Random.nextInt(-5, 15), // minRankImprovement 이상의 값이 나올 수 있도록 범위 조정,
                        score = Random.nextDouble(0.0, 100.0),
                        growthRate = growthRate,
                        growthConsistency = Random.nextDouble(0.0, 1.0),
                        growthFactors =
                            SkillGrowthRankingsDocument.SkillGrowthRankingEntry.GrowthFactors(
                                demandGrowth = growthRate,
                                salaryGrowth = Random.nextDouble(0.0, 20.0),
                                adoptionRate = Random.nextDouble(0.0, 1.0),
                                marketPenetration = Random.nextDouble(0.0, 1.0),
                            ),
                    ),
                ),
        )

    @Test
    @Order(1)
    fun testBulkInsert() {
        startTime = System.currentTimeMillis()
        allRecords.clear()

        val baseDates =
            (1..12).map { month ->
                val monthStr = month.toString().padStart(2, '0')
                "2024$monthStr"
            }

        var totalInserted = 0
        var rank = 1
        for (baseDate in baseDates) {
            val records =
                (1..totalRecords / 12).map {
                    createSkillGrowthDocument(
                        baseDate = baseDate,
                        rank = rank++,
                        entityId = Random.nextLong(1000, 9999),
                        growthRate = Random.nextDouble(-20.0, 50.0),
                    )
                }
            val result = skillGrowthRankingsRepository.bulkInsert(records)
            totalInserted += result.size
            allRecords.addAll(result)
        }

        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

    @Test
    @Order(4)
    fun testFindByGrowthRate() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val minGrowthRate = 20.0
        val results = skillGrowthRankingsRepository.findByGrowthRate(baseDate, minGrowthRate)

        Assertions.assertTrue(results.isNotEmpty())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by growth rate execution time: $timeSeconds seconds")
        performanceMetrics["find_by_growth_rate"] = timeSeconds
    }

    @Test
    @Order(5)
    fun testFindEntitiesWithConsistentGrowth() {
        startTime = System.currentTimeMillis()

        val months = 3
        val minGrowthRate = 15.0
        val results = skillGrowthRankingsRepository.findEntitiesWithConsistentGrowth(months, minGrowthRate)

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find consistent growth entities execution time: $timeSeconds seconds")
        performanceMetrics["find_consistent_growth"] = timeSeconds
    }

    @Test
    @Order(6)
    fun testFindRisingStars() {
        startTime = System.currentTimeMillis()

        // 먼저 데이터가 제대로 들어갔는지 확인
        val allDocs = skillGrowthRankingsRepository.findAllByQuery(Query())
        logger.info("Total documents: ${allDocs.size}")
        logger.info("Sample rank changes: ${allDocs.take(5).map { it.rankings.first().rankChange }}")

        val months = 3
        val minRankImprovement = 10
        val results = skillGrowthRankingsRepository.findRisingStars(months, minRankImprovement)

        logger.info("Found ${results.size} rising stars")
        results.take(5).forEach { doc ->
            logger.info("Rank change: ${doc.rankings.first().rankChange}")
        }

        Assertions.assertTrue(results.isNotEmpty(), "No rising stars found")
        Assertions.assertTrue(
            results.all { doc ->
                val rankChange = doc.rankings.first().rankChange
                rankChange != null && rankChange >= minRankImprovement
            },
            "Found documents with insufficient rank improvement",
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find rising stars execution time: $timeSeconds seconds")
        performanceMetrics["find_rising_stars"] = timeSeconds
    }

//    @Test
//    @Order(7)
//    fun testFindTrendingEntities() {
//        startTime = System.currentTimeMillis()
//
//        val months = 3
//        val results = skillGrowthRankingsRepository.findTrendingEntities(months)
//
//        Assertions.assertTrue(results.isNotEmpty())
//
//        val endTime = System.currentTimeMillis()
//        val timeSeconds = (endTime - startTime) / 1000.0
//        logger.info("Find trending entities execution time: $timeSeconds seconds")
//        performanceMetrics["find_trending_entities"] = timeSeconds
//    }

    @Test
    @Order(8)
    fun testFindByEntityIdAndBaseDate() {
        startTime = System.currentTimeMillis()

        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val baseDate = BaseDate("202401")
        val result = skillGrowthRankingsRepository.findByEntityIdAndBaseDate(entityId, baseDate)

        Assertions.assertNotNull(result)
        result?.let {
            Assertions.assertEquals(entityId, it.rankings.first().entityId)
            Assertions.assertEquals(baseDate.toString(), it.baseDate)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by entity ID and base date execution time: $timeSeconds seconds")
        performanceMetrics["find_by_entity_and_date"] = timeSeconds
    }

    @Test
    @Order(9)
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        var totalDeleted = 0

        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = skillGrowthRankingsRepository.bulkDelete(batch)
            totalDeleted += deletedCount
        }

        val remainingRecords = skillGrowthRankingsRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }
}
