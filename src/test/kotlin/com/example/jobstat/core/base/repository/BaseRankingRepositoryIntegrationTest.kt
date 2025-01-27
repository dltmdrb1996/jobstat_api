package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.rankings.model.SkillGrowthRankingsDocument
import com.example.jobstat.rankings.repository.SkillGrowthRankingsRepositoryImpl
import com.example.jobstat.utils.TestUtils
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BaseRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var skillGrowthRankingsRepository: SkillGrowthRankingsRepositoryImpl

    private val totalRecords = 96 // 테스트 간소화를 위해 조정
    private val batchSize = 10
    private val allRecords = mutableListOf<SkillGrowthRankingsDocument>()
    private val performanceMetrics = hashMapOf<String, Double>()
    private var startTime: Long = 0

    // 고정된 시드 사용
    private val random = Random(12345)

    override fun cleanupTestData() {
        val recordIds = allRecords.mapNotNull { it.id }
        if (recordIds.isNotEmpty()) {
            for (batch in recordIds.chunked(batchSize)) {
                skillGrowthRankingsRepository.bulkDelete(batch)
            }
        }
    }

    @BeforeAll
    fun beforeAll() {
        // 필요한 경우 초기 설정
    }

    @AfterAll
    fun afterAll() {
        cleanupTestData()
        printExecutionSummary()
    }

    @AfterEach
    fun teardown() {
        // 각 테스트 후 메모리 사용량 로그
        TestUtils.logMemoryUsage()
        logger.info("Test completed in ${(System.currentTimeMillis() - testStartTime) / 1000.0} seconds")
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
                    totalCount = totalRecords,
                    rankedCount = totalRecords,
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

    @Test
    @Order(1)
    fun testBulkInsert() {
        startTime = System.currentTimeMillis()
        allRecords.clear()

        val baseDates = listOf("202401", "202402", "202403", "202404", "202405", "202406")

        var totalInserted = 0
        for (baseDate in baseDates) {
            val records =
                (1..(totalRecords / baseDates.size)).map { rank ->
                    val entityId = (baseDate.toInt() * 100 + rank).toLong()
                    createSkillDocument(baseDate, rank, entityId)
                }
            val result = skillGrowthRankingsRepository.bulkInsert(records)
            totalInserted += result.size
            allRecords.addAll(result)
        }

        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        // 추가 검증: 삽입된 데이터가 실제로 DB에 존재하는지 확인
        val insertedCount = skillGrowthRankingsRepository.findAllByQuery(Query()).size
        Assertions.assertEquals(totalRecords, insertedCount, "DB contains a different number of records than inserted")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

    @Test
    @Order(2)
    fun testFindTopN() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val limit = 10
        val topSkills = skillGrowthRankingsRepository.findTopN(baseDate, limit)

        Assertions.assertEquals(limit, topSkills.size, "Top N size mismatch")
        Assertions.assertTrue(
            topSkills.all { it.rankings.first().rank <= limit },
            "Top N ranks exceed limit",
        )
        // Verify ranks are in ascending order
        Assertions.assertEquals(
            topSkills.map { it.rankings.first().rank },
            topSkills.map { it.rankings.first().rank }.sorted(),
            "Top N ranks are not in ascending order",
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

        val baseDate = BaseDate("202401")
        val startRank = 5
        val endRank = 15
        val skills = skillGrowthRankingsRepository.findByRankRange(baseDate, startRank, endRank)

        Assertions.assertTrue(skills.isNotEmpty(), "Find by rank range returned empty")
        Assertions.assertTrue(
            skills.all { it.rankings.first().rank in startRank..endRank },
            "Some ranks are outside the specified range",
        )
        // Verify ranks are in ascending order
        Assertions.assertEquals(
            skills.map { it.rankings.first().rank },
            skills.map { it.rankings.first().rank }.sorted(),
            "Ranks are not in ascending order",
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

        val baseDate = BaseDate("202401")
        val limit = 10
        val movers = skillGrowthRankingsRepository.findTopMovers(baseDate, limit)

        Assertions.assertTrue(movers.isNotEmpty(), "Find top movers returned empty")
        Assertions.assertTrue(movers.size <= limit, "Find top movers exceeded limit")
        // Verify rank changes are in descending order
        Assertions.assertEquals(
            movers.map { it.rankings.first().rankChange },
            movers.map { it.rankings.first().rankChange }.sortedByDescending { it },
            "Top movers are not in descending order of rank change",
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

        val baseDate = BaseDate("202401")
        val limit = 10
        val losers = skillGrowthRankingsRepository.findTopLosers(baseDate, limit)

        Assertions.assertTrue(losers.isNotEmpty(), "Find top losers returned empty")
        Assertions.assertTrue(losers.size <= limit, "Find top losers exceeded limit")
        // Verify rank changes are in ascending order (most negative first)
        Assertions.assertEquals(
            losers.map { it.rankings.first().rankChange },
            losers.map { it.rankings.first().rankChange }.sortedBy { it },
            "Top losers are not in ascending order of rank change",
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find top losers execution time: $timeSeconds seconds")
        performanceMetrics["find_top_losers"] = timeSeconds
    }

    @Test
    @Order(6)
    fun testFindVolatileEntities() {
        startTime = System.currentTimeMillis()

        val months = 3
        val minRankChange = 1 // 조정된 최소 rank change
        val volatileSkills = skillGrowthRankingsRepository.findVolatileEntities(months, minRankChange)

        Assertions.assertTrue(
            volatileSkills.all { skill ->
                val rankChange = skill.rankings.first().rankChange
                rankChange != null && kotlin.math.abs(rankChange) >= minRankChange
            },
            "Some volatile skills do not meet the minimum rank change criteria",
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find volatile entities execution time: $timeSeconds seconds")
        performanceMetrics["find_volatile_entities"] = timeSeconds
    }

    @Test
    @Order(7)
    fun testFindEntitiesWithConsistentRanking() {
        startTime = System.currentTimeMillis()

        val months = 3
        val maxRank = 10
        val consistentSkills = skillGrowthRankingsRepository.findEntitiesWithConsistentRanking(months, maxRank)

        Assertions.assertTrue(
            consistentSkills.all { it.rankings.first().rank <= maxRank },
            "Some consistent skills exceed the maximum rank",
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find consistent ranking entities execution time: $timeSeconds seconds")
        performanceMetrics["find_consistent_ranking"] = timeSeconds
    }

    @Test
    @Order(8)
    fun testFindRankingHistory() {
        startTime = System.currentTimeMillis()

        val months = 6
        val sampleEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val history = skillGrowthRankingsRepository.findRankingHistory(sampleEntityId, months)

        Assertions.assertTrue(history.isNotEmpty(), "Find ranking history returned empty")
        Assertions.assertTrue(history.size <= months, "Find ranking history exceeded the specified months")
        Assertions.assertTrue(
            history.all { it.rankings.first().entityId == sampleEntityId },
            "Some history entries do not match the sample entity ID",
        )
        // Verify dates are in descending order
        Assertions.assertTrue(
            history.map { it.baseDate }.zipWithNext().all { (a, b) -> a >= b },
            "Ranking history is not in descending order of baseDate",
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find ranking history execution time: $timeSeconds seconds")
        performanceMetrics["find_ranking_history"] = timeSeconds
    }

    @Test
    @Order(11)
    fun testFindTopNWithLimitExceedingTotal() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val limit = 20 // totalRecords / baseDates.size = 16
        val topSkills = skillGrowthRankingsRepository.findTopN(baseDate, limit)

        Assertions.assertEquals(16, topSkills.size, "Top N should return all available records when limit exceeds total")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find top N with limit exceeding total execution time: $timeSeconds seconds")
        performanceMetrics["find_top_n_limit_exceed"] = timeSeconds
    }

    @Test
    @Order(12)
    fun testFindByRankRangeWithNoResults() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val startRank = 100 // 존재하지 않는 rank 범위
        val endRank = 200
        val skills = skillGrowthRankingsRepository.findByRankRange(baseDate, startRank, endRank)

        Assertions.assertTrue(skills.isEmpty(), "Find by rank range should return empty list for non-existing ranks")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by rank range with no results execution time: $timeSeconds seconds")
        performanceMetrics["find_by_rank_range_no_results"] = timeSeconds
    }

    @Test
    @Order(13)
    fun testFindTopNWithInvalidBaseDate() {
        Assertions.assertThrows(Exception::class.java) {
            skillGrowthRankingsRepository.findTopN(BaseDate("invalid_date"), 10)
        }
    }

    @Test
    @Order(14)
    fun testFindByRankRangeWithInvalidRanks() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val startRank = 15
        val endRank = 5
        val skills = skillGrowthRankingsRepository.findByRankRange(baseDate, startRank, endRank)

        Assertions.assertTrue(skills.isEmpty(), "Find by rank range should return empty list for invalid rank range")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by rank range with invalid ranks execution time: $timeSeconds seconds")
        performanceMetrics["find_by_rank_range_invalid"] = timeSeconds
    }

    @Test
    @Order(16)
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        var totalDeleted = 0

        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = skillGrowthRankingsRepository.bulkDelete(batch)
            totalDeleted += deletedCount
        }

        Assertions.assertEquals(totalRecords, totalDeleted, "Bulk delete count mismatch")

        val remainingRecords = skillGrowthRankingsRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty(), "Remaining records should be empty after bulk delete")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }

    @Test
    @Order(17)
    fun testFinalVerification() {
        startTime = System.currentTimeMillis()

        logger.info("Performance metrics summary:")
        performanceMetrics.forEach { (operation, time) ->
            logger.info("$operation: $time seconds")
        }

        // Verify all test data has been cleaned up
        val remainingRecords = skillGrowthRankingsRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty(), "Final verification failed: remaining records exist")

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
