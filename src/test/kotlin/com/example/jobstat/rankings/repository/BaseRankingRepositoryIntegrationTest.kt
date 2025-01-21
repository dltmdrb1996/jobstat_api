package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.rankings.model.SkillGrowthRankingsDocument
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random

@TestMethodOrder(OrderAnnotation::class)
class BaseRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
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

    private fun createSkillDocument(
        baseDate: String,
        rank: Int,
        entityId: Long,
    ): SkillGrowthRankingsDocument {
        val growthRate = Random.nextDouble(-20.0, 50.0)
        val consistency = Random.nextDouble(0.0, 1.0)
        val previousRank = rank + Random.nextInt(-5, 5)

        return SkillGrowthRankingsDocument(
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
                        previousRank = previousRank,
                        rankChange = previousRank - rank,
                        score = Random.nextDouble(0.0, 100.0),
                        growthRate = growthRate,
                        growthConsistency = consistency,
                        growthFactors =
                            SkillGrowthRankingsDocument.SkillGrowthRankingEntry.GrowthFactors(
                                demandGrowth = Random.nextDouble(0.0, 30.0),
                                salaryGrowth = Random.nextDouble(0.0, 20.0),
                                adoptionRate = Random.nextDouble(0.0, 1.0),
                                marketPenetration = Random.nextDouble(0.0, 1.0),
                            ),
                    ),
                ),
        )
    }

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
                    createSkillDocument(baseDate, rank++, Random.nextLong(1000, 9999))
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
    @Order(2)
    fun testFindTopN() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val limit = 10
        val topSkills = skillGrowthRankingsRepository.findTopN(baseDate, limit)

        Assertions.assertEquals(limit, topSkills.size)
        Assertions.assertTrue(
            topSkills.all { it.rankings.first().rank <= limit },
        )
        // Verify ranks are in ascending order
        Assertions.assertEquals(
            topSkills.map { it.rankings.first().rank },
            topSkills.map { it.rankings.first().rank }.sorted(),
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find top N execution time: $timeSeconds seconds")
        performanceMetrics["find_top_n"] = timeSeconds
    }

    @Test
    @Order(3)
    fun testFindByRankRange() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val startRank = 5
        val endRank = 15
        val skills = skillGrowthRankingsRepository.findByRankRange(baseDate, startRank, endRank)

        Assertions.assertTrue(skills.isNotEmpty())
        Assertions.assertTrue(
            skills.all { it.rankings.first().rank in startRank..endRank },
        )
        // Verify ranks are in ascending order
        Assertions.assertEquals(
            skills.map { it.rankings.first().rank },
            skills.map { it.rankings.first().rank }.sorted(),
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by rank range execution time: $timeSeconds seconds")
        performanceMetrics["find_by_rank_range"] = timeSeconds
    }

    @Test
    @Order(4)
    fun testFindTopMovers() {
        startTime = System.currentTimeMillis()

        val startDate = "202401"
        val endDate = "202402"
        val limit = 10
        val movers = skillGrowthRankingsRepository.findTopMovers(startDate, endDate, limit)

        Assertions.assertTrue(movers.isNotEmpty())
        Assertions.assertTrue(movers.size <= limit)
        // Verify rank changes are in descending order
        Assertions.assertEquals(
            movers.map { it.rankings.first().rankChange },
            movers.map { it.rankings.first().rankChange }.sortedByDescending { it },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find top movers execution time: $timeSeconds seconds")
        performanceMetrics["find_top_movers"] = timeSeconds
    }

    @Test
    @Order(5)
    fun testFindTopLosers() {
        startTime = System.currentTimeMillis()

        val startDate = "202401"
        val endDate = "202402"
        val limit = 10
        val losers = skillGrowthRankingsRepository.findTopLosers(startDate, endDate, limit)

        Assertions.assertTrue(losers.isNotEmpty())
        Assertions.assertTrue(losers.size <= limit)
        // Verify rank changes are in ascending order (most negative first)
        Assertions.assertEquals(
            losers.map { it.rankings.first().rankChange },
            losers.map { it.rankings.first().rankChange }.sortedBy { it },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find top losers execution time: $timeSeconds seconds")
        performanceMetrics["find_top_losers"] = timeSeconds
    }

    @Test
    @Order(6)
    fun testFindStableEntities() {
        startTime = System.currentTimeMillis()

        val months = 3
        val maxRankChange = 5
        val stableSkills = skillGrowthRankingsRepository.findStableEntities(months, maxRankChange)

        Assertions.assertTrue(
            stableSkills.all { skill ->
                val rankChange = skill.rankings.first().rankChange
                rankChange == null || abs(rankChange) <= maxRankChange
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find stable entities execution time: $timeSeconds seconds")
        performanceMetrics["find_stable_entities"] = timeSeconds
    }

    @Test
    @Order(7)
    fun testFindVolatileEntities() {
        startTime = System.currentTimeMillis()

        val months = 3
        val minRankChange = 10
        val volatileSkills = skillGrowthRankingsRepository.findVolatileEntities(months, minRankChange)

        Assertions.assertTrue(
            volatileSkills.all { skill ->
                val rankChange = skill.rankings.first().rankChange
                rankChange == null || abs(rankChange) >= minRankChange
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find volatile entities execution time: $timeSeconds seconds")
        performanceMetrics["find_volatile_entities"] = timeSeconds
    }

    @Test
    @Order(8)
    fun testFindEntitiesWithConsistentRanking() {
        startTime = System.currentTimeMillis()

        val months = 3
        val maxRank = 10
        val consistentSkills = skillGrowthRankingsRepository.findEntitiesWithConsistentRanking(months, maxRank)

        Assertions.assertTrue(
            consistentSkills.all { it.rankings.first().rank <= maxRank },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find consistent ranking entities execution time: $timeSeconds seconds")
        performanceMetrics["find_consistent_ranking"] = timeSeconds
    }

    @Test
    @Order(9)
    fun testFindRankingHistory() {
        startTime = System.currentTimeMillis()

        val months = 3
        val sampleEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val history = skillGrowthRankingsRepository.findRankingHistory(sampleEntityId, months)

        Assertions.assertTrue(history.isNotEmpty())
        Assertions.assertTrue(history.size <= months)
        Assertions.assertTrue(
            history.all { it.rankings.first().entityId == sampleEntityId },
        )
        // Verify dates are in descending order
        Assertions.assertEquals(
            history.map { it.baseDate },
            history.map { it.baseDate }.sortedDescending(),
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find ranking history execution time: $timeSeconds seconds")
        performanceMetrics["find_ranking_history"] = timeSeconds
    }

    @Test
    @Order(10)
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

    @Test
    @Order(11)
    fun testFinalVerification() {
        startTime = System.currentTimeMillis()

        logger.info("Performance metrics summary:")
        performanceMetrics.forEach { (operation, time) ->
            logger.info("$operation: $time seconds")
        }

        // Verify all test data has been cleaned up
        val remainingRecords = skillGrowthRankingsRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty())

        // Print total test execution time
        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Final verification execution time: $timeSeconds seconds")

        // Additional verification of performance thresholds
        performanceMetrics.forEach { (operation, time) ->
            when (operation) {
                "bulk_insert" -> Assertions.assertTrue(time < 30.0, "Bulk insert took too long: $time seconds")
                "find_top_n" -> Assertions.assertTrue(time < 5.0, "Find top N took too long: $time seconds")
                "find_by_rank_range" ->
                    Assertions.assertTrue(
                        time < 5.0,
                        "Find by rank range took too long: $time seconds",
                    )

                "find_top_movers" -> Assertions.assertTrue(time < 5.0, "Find top movers took too long: $time seconds")
                "find_top_losers" -> Assertions.assertTrue(time < 5.0, "Find top losers took too long: $time seconds")
                "find_stable_entities" ->
                    Assertions.assertTrue(
                        time < 10.0,
                        "Find stable entities took too long: $time seconds",
                    )

                "find_volatile_entities" ->
                    Assertions.assertTrue(
                        time < 10.0,
                        "Find volatile entities took too long: $time seconds",
                    )

                "find_consistent_ranking" ->
                    Assertions.assertTrue(
                        time < 10.0,
                        "Find consistent ranking took too long: $time seconds",
                    )

                "find_ranking_history" ->
                    Assertions.assertTrue(
                        time < 5.0,
                        "Find ranking history took too long: $time seconds",
                    )

                "bulk_delete" -> Assertions.assertTrue(time < 20.0, "Bulk delete took too long: $time seconds")
            }
        }
    }
}
