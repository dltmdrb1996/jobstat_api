package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.DistributionRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.state.EntityType
import com.example.jobstat.rankings.model.CompanySizeEducationRankingsDocument
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(OrderAnnotation::class)
class DistributionRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var companySizeEducationRepository: CompanySizeEducationRankingsRepositoryImpl

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<CompanySizeEducationRankingsDocument>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()
    private val educationLevel = listOf("HIGH_SCHOOL", "ASSOCIATE", "BACHELOR", "MASTER", "DOCTORATE")

    override fun cleanupTestData() {
        val recordIds = allRecords.mapNotNull { it.id }
        for (batch in recordIds.chunked(batchSize)) {
            companySizeEducationRepository.bulkDelete(batch)
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

    private fun createRandomDistribution(): Map<String, Double> {
        val values = educationLevel.map { Random.nextDouble(0.0, 1.0) }
        val sum = values.sum()
        return educationLevel.zip(values.map { it / sum }).toMap()
    }

    private fun createEducationDocument(
        baseDate: String,
        rank: Int,
        entityId: Long,
    ): CompanySizeEducationRankingsDocument {
        val distribution = createRandomDistribution()
        val dominantCategory = distribution.maxByOrNull { it.value }?.key ?: educationLevel.first()

        return CompanySizeEducationRankingsDocument(
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            metrics =
                CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics(
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
                    educationTrends =
                        CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics.EducationTrends(
                            overallDistribution = distribution,
                            yearOverYearChanges = distribution.mapValues { Random.nextDouble(-0.2, 0.2) },
                            marketComparison = distribution.mapValues { Random.nextDouble(0.8, 1.2) },
                            industryPatterns =
                                listOf(
                                    CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics.EducationTrends.IndustryPattern(
                                        industryId = Random.nextLong(1, 100),
                                        industryName = "Industry_${Random.nextInt(100)}",
                                        distribution = distribution,
                                    ),
                                ),
                        ),
                ),
            groupEntityType = EntityType.COMPANY_SIZE,
            targetEntityType = EntityType.EDUCATION,
            rankings =
                listOf(
                    CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry(
                        documentId = "Company_Size_$entityId",
                        entityId = entityId,
                        name = "Company_Size_$entityId",
                        rank = rank,
                        previousRank = rank + Random.nextInt(-5, 5),
                        rankChange = Random.nextInt(-5, 5),
                        distribution = distribution,
                        dominantCategory = dominantCategory,
                        distributionMetrics =
                            DistributionRankingDocument.DistributionMetrics(
                                entropy = Random.nextDouble(0.0, 1.0),
                                concentration = Random.nextDouble(0.0, 1.0),
                                uniformity = Random.nextDouble(0.0, 1.0),
                            ),
                        totalPostings = Random.nextInt(1000, 10000),
                        educationRequirements =
                            CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.EducationRequirements(
                                mandatoryRatio = Random.nextDouble(0.0, 1.0),
                                preferredRatio = Random.nextDouble(0.0, 1.0),
                                flexibleRatio = Random.nextDouble(0.0, 1.0),
                                requirementsByJobLevel =
                                    mapOf(
                                        "ENTRY" to distribution,
                                        "MID" to distribution,
                                        "SENIOR" to distribution,
                                    ),
                            ),
                        salaryDistribution =
                            educationLevel.associateWith {
                                CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics(
                                    avgSalary = Random.nextLong(50000, 150000),
                                    medianSalary = Random.nextLong(45000, 140000),
                                    salaryRange =
                                        CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics.SalaryRange(
                                            min = Random.nextLong(30000, 50000),
                                            max = Random.nextLong(150000, 200000),
                                            p25 = Random.nextLong(40000, 60000),
                                            p75 = Random.nextLong(120000, 140000),
                                        ),
                                )
                            },
                        trendIndicators =
                            CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.TrendIndicators(
                                growthRate = Random.nextDouble(-0.2, 0.5),
                                changeVelocity = Random.nextDouble(0.0, 1.0),
                                stabilityScore = Random.nextDouble(0.0, 1.0),
                                futureProjection = if (Random.nextBoolean()) "GROWTH" else "STABLE",
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
                    createEducationDocument(baseDate, rank++, Random.nextLong(1000, 9999))
                }
            val result = companySizeEducationRepository.bulkInsert(records)
            totalInserted += result.size
            allRecords.addAll(result)
        }

        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

//    @Test
//    @Order(2)
//    fun testFindByDistributionPattern() {
//        startTime = System.currentTimeMillis()
//
//        val baseDate = "202401"
//        val pattern = mapOf(
//            "BACHELOR" to 0.5,
//            "MASTER" to 0.3,
//            "DOCTORATE" to 0.2
//        )
//        val threshold = 0.2
//
//        val results = companySizeEducationRepository.findByDistributionPattern(baseDate, pattern, threshold)
//
//        Assertions.assertTrue(results.isNotEmpty())
//        // Verify similarity is within threshold
//        results.forEach { doc ->
//            val actualDist = doc.rankings.first().distribution
//            val similarity = pattern.keys.sumOf {
//                abs(actualDist[it]!! - pattern[it]!!)
//            }
//            Assertions.assertTrue(similarity <= threshold)
//        }
//
//        val endTime = System.currentTimeMillis()
//        val timeSeconds = (endTime - startTime) / 1000.0
//        logger.info("Find by distribution pattern execution time: $timeSeconds seconds")
//        performanceMetrics["find_by_distribution_pattern"] = timeSeconds
//    }

    @Test
    @Order(3)
    fun testFindByDominantCategory() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val category = "BACHELOR"

        val results = companySizeEducationRepository.findByDominantCategory(baseDate, category)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all { it.rankings.first().dominantCategory == category },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by dominant category execution time: $timeSeconds seconds")
        performanceMetrics["find_by_dominant_category"] = timeSeconds
    }

    @Test
    @Order(4)
    fun testFindDistributionTrends() {
        startTime = System.currentTimeMillis()

        val months = 3
        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val results = companySizeEducationRepository.findDistributionTrends(entityId, months)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= months)
        Assertions.assertTrue(
            results.all { it.rankings.first().entityId == entityId },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find distribution trends execution time: $timeSeconds seconds")
        performanceMetrics["find_distribution_trends"] = timeSeconds
    }

//    @Test
//    @Order(5)
//    fun testFindSignificantDistributionChanges() {
//        startTime = System.currentTimeMillis()
//
//        val startDate = "202401"
//        val endDate = "202402"
//
//        val results = companySizeEducationRepository.findSignificantDistributionChanges(startDate, endDate)
//
//        Assertions.assertTrue(results.isNotEmpty())
//        // Results should contain entities with significant changes in their distribution
//
//        val endTime = System.currentTimeMillis()
//        val timeSeconds = (endTime - startTime) / 1000.0
//        logger.info("Find significant distribution changes execution time: $timeSeconds seconds")
//        performanceMetrics["find_significant_changes"] = timeSeconds
//    }

//    @Test
//    @Order(6)
//    fun testFindSimilarDistributions() {
//        startTime = System.currentTimeMillis()
//
//        val baseDate = "202401"
//        val entityId = allRecords.first().rankings.first().entityId
//        val similarity = 0.8
//
//        val results = companySizeEducationRepository.findSimilarDistributions(entityId, baseDate, similarity)
//
//        Assertions.assertTrue(results.isNotEmpty())
//        // Results should not include the target entity
//        Assertions.assertTrue(
//            results.none { it.rankings.first().entityId == entityId }
//        )
//
//        val endTime = System.currentTimeMillis()
//        val timeSeconds = (endTime - startTime) / 1000.0
//        logger.info("Find similar distributions execution time: $timeSeconds seconds")
//        performanceMetrics["find_similar_distributions"] = timeSeconds
//    }

    @Test
    @Order(7)
    fun testFindUniformDistributions() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val maxVariance = 0.3

        val results = companySizeEducationRepository.findUniformDistributions(baseDate, maxVariance)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all {
                it.rankings
                    .first()
                    .distributionMetrics.uniformity <= maxVariance
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find uniform distributions execution time: $timeSeconds seconds")
        performanceMetrics["find_uniform_distributions"] = timeSeconds
    }

    @Test
    @Order(8)
    fun testFindSkewedDistributions() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val minSkewness = 0.7

        val results = companySizeEducationRepository.findSkewedDistributions(baseDate, minSkewness)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all {
                it.rankings
                    .first()
                    .distributionMetrics.concentration >= minSkewness
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find skewed distributions execution time: $timeSeconds seconds")
        performanceMetrics["find_skewed_distributions"] = timeSeconds
    }

    @Test
    @Order(9)
    fun testFindDistributionChanges() {
        startTime = System.currentTimeMillis()

        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val months = 3

        val results = companySizeEducationRepository.findDistributionChanges(entityId, months)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= months)
        Assertions.assertTrue(
            results.all { it.rankings.first().entityId == entityId },
        )
        // Verify results are ordered by date descending
        Assertions.assertEquals(
            results.map { it.baseDate },
            results.map { it.baseDate }.sortedDescending(),
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find distribution changes execution time: $timeSeconds seconds")
        performanceMetrics["find_distribution_changes"] = timeSeconds
    }

    @Test
    @Order(10)
    fun testFindCategoryDominance() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val category = "BACHELOR"
        val minPercentage = 0.1

        val results = companySizeEducationRepository.findCategoryDominance(baseDate, category, minPercentage)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all {
                it.rankings.first().distribution[category]!! >= minPercentage
            },
        )
        // Verify results are ordered by category percentage descending
        val percentages = results.map { it.rankings.first().distribution[category]!! }
        Assertions.assertEquals(percentages, percentages.sortedDescending())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find category dominance execution time: $timeSeconds seconds")
        performanceMetrics["find_category_dominance"] = timeSeconds
    }

    @Test
    @Order(11)
    fun testFindByEntityIdAndBaseDate() {
        startTime = System.currentTimeMillis()

        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val baseDate = "202401"

        val result = companySizeEducationRepository.findByEntityIdAndBaseDate(entityId, baseDate)

        Assertions.assertNotNull(result)
        result?.let {
            Assertions.assertEquals(entityId, it.rankings.first().entityId)
            Assertions.assertEquals(baseDate, it.baseDate)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by entity ID and base date execution time: $timeSeconds seconds")
        performanceMetrics["find_by_entity_and_date"] = timeSeconds
    }

    @Test
    @Order(12)
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        var totalDeleted = 0

        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = companySizeEducationRepository.bulkDelete(batch)
            totalDeleted += deletedCount
        }

        val remainingRecords = companySizeEducationRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }
}
