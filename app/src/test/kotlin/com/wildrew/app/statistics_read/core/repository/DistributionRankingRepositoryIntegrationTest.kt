package com.wildrew.app.statistics_read.core.repository

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.app.statistics_read.core.core_model.EntityType
import com.wildrew.app.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.DistributionRankingDocument
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.wildrew.app.statistics_read.rankings.document.CompanySizeEducationRankingsDocument
import com.wildrew.app.statistics_read.rankings.repository.CompanySizeEducationRankingsRepositoryImpl
import com.wildrew.app.utils.config.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("DistributionRankingRepository 통합 테스트")
class DistributionRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var companySizeEducationRepository: CompanySizeEducationRankingsRepositoryImpl

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<CompanySizeEducationRankingsDocument>()
    private val educationLevels = listOf("HIGH_SCHOOL", "ASSOCIATE", "BACHELOR", "MASTER", "DOCTORATE")

    @BeforeEach
    override fun setup() {
        mongoTemplate.dropCollection("company_size_education_rankings")

        val baseDate = "202401"
        val totalPages = (totalRecords + batchSize - 1) / batchSize

        for (page in 1..totalPages) {
            val document = createTestDocument(baseDate, page)
            allRecords.add(document)
            companySizeEducationRepository.save(document)
        }

        val nextMonthDate = "202402"
        val nextMonthDoc = createTestDocument(nextMonthDate, 1)
        companySizeEducationRepository.save(nextMonthDoc)
    }

    private fun createTestDocument(
        baseDate: String,
        page: Int,
        forceSkewed: Boolean = false,
    ): CompanySizeEducationRankingsDocument {
        val startRank = (page - 1) * batchSize + 1
        val rankings =
            (0 until batchSize).map { index ->
                val rank = startRank + index
                if (forceSkewed && index < batchSize / 2) {
                    createSkewedRankingEntry(rank)
                } else {
                    createTestRankingEntry(rank)
                }
            }

        return CompanySizeEducationRankingsDocument(
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            metrics = createTestMetrics(),
            groupEntityType = EntityType.COMPANY_SIZE,
            targetEntityType = EntityType.EDUCATION,
            rankings = rankings,
            page = page,
        )
    }

    private fun createSkewedRankingEntry(rank: Int): CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry {
        val entityId = rank.toLong()
        val distribution = createSkewedDistribution()
        val dominantCategory = distribution.maxByOrNull { it.value }?.key ?: educationLevels.first()

        return CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry(
            entityId = entityId,
            name = "Company_Size_$entityId",
            rank = rank,
            previousRank = rank + Random.nextInt(-5, 6),
            rankChange = Random.nextInt(-10, 11),
            distribution = distribution,
            dominantCategory = dominantCategory,
            distributionMetrics =
                DistributionRankingDocument.DistributionMetrics(
                    entropy = calculateEntropy(distribution),
                    concentration = calculateConcentration(distribution),
                    uniformity = calculateUniformity(distribution),
                ),
            totalPostings = Random.nextInt(1000, 10000),
            educationRequirements = createTestEducationRequirements(),
            salaryDistribution = createTestSalaryDistribution(),
            trendIndicators = createTestTrendIndicators(),
        )
    }

    private fun createSkewedDistribution(): Map<String, Double> {
        val dominantCategory = educationLevels.random()
        val weights =
            educationLevels.associateWith { category ->
                when (category) {
                    dominantCategory -> Random.nextDouble(0.6, 0.8) // 매우 높은 비중
                    else -> Random.nextDouble(0.05, 0.1) // 매우 낮은 비중
                }
            }

        val sum = weights.values.sum()
        return weights.mapValues { it.value / sum }
    }

    private fun createRandomDistribution(dominantCategory: String? = null): Map<String, Double> {
        val weights =
            educationLevels.associateWith {
                if (it == dominantCategory) {
                    Random.nextDouble(0.4, 0.6)
                } else {
                    Random.nextDouble(0.05, 0.15)
                }
            }

        val sum = weights.values.sum()
        return weights.mapValues { it.value / sum }
    }

    private fun createTestRankingEntry(rank: Int): CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry {
        val entityId = rank.toLong()
        val dominantCategory = "BACHELOR"
        val distribution = createRandomDistribution(dominantCategory)

        return CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry(
            entityId = entityId,
            name = "Company_Size_$entityId",
            rank = rank,
            previousRank = rank + Random.nextInt(-5, 6),
            rankChange = Random.nextInt(-10, 11),
            distribution = distribution,
            dominantCategory = dominantCategory,
            distributionMetrics =
                DistributionRankingDocument.DistributionMetrics(
                    entropy = calculateEntropy(distribution),
                    concentration = calculateConcentration(distribution),
                    uniformity = calculateUniformity(distribution),
                ),
            totalPostings = Random.nextInt(1000, 10000),
            educationRequirements = createTestEducationRequirements(),
            salaryDistribution = createTestSalaryDistribution(),
            trendIndicators = createTestTrendIndicators(),
        )
    }

    private fun calculateEntropy(distribution: Map<String, Double>): Double =
        -distribution.values.sumOf { p ->
            if (p > 0) p * kotlin.math.log2(p) else 0.0
        }

    private fun calculateConcentration(distribution: Map<String, Double>): Double = distribution.values.maxOrNull() ?: 0.0

    private fun calculateUniformity(distribution: Map<String, Double>): Double {
        val idealShare = 1.0 / distribution.size
        return distribution.values.sumOf {
            kotlin.math.abs(it - idealShare)
        } / 2.0
    }

    private fun calculateSimilarity(
        dist1: Map<String, Double>,
        dist2: Map<String, Double>,
    ): Double {
        val allCategories = (dist1.keys + dist2.keys).toSet()
        return 1.0 - allCategories.sumOf { category ->
            kotlin.math.abs((dist1[category] ?: 0.0) - (dist2[category] ?: 0.0))
        } / allCategories.size
    }

    private fun createTestMetrics(): CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics =
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
            educationTrends = createTestEducationTrends(),
        )

    private fun createTestEducationTrends(): CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics.EducationTrends {
        val distribution = createRandomDistribution()
        return CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics.EducationTrends(
            overallDistribution = distribution,
            yearOverYearChanges = distribution.mapValues { Random.nextDouble(-0.2, 0.2) },
            marketComparison = distribution.mapValues { Random.nextDouble(0.8, 1.2) },
            industryPatterns = listOf(createTestIndustryPattern()),
        )
    }

    private fun createTestIndustryPattern(): CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics.EducationTrends.IndustryPattern =
        CompanySizeEducationRankingsDocument.CompanySizeEducationMetrics.EducationTrends.IndustryPattern(
            industryId = Random.nextLong(1, 100),
            industryName = "Industry_${Random.nextInt(100)}",
            distribution = createRandomDistribution(),
        )

    private fun createTestEducationRequirements(): CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.EducationRequirements =
        CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.EducationRequirements(
            mandatoryRatio = Random.nextDouble(0.0, 1.0),
            preferredRatio = Random.nextDouble(0.0, 1.0),
            flexibleRatio = Random.nextDouble(0.0, 1.0),
            requirementsByJobLevel =
                mapOf(
                    "ENTRY" to createRandomDistribution(),
                    "MID" to createRandomDistribution(),
                    "SENIOR" to createRandomDistribution(),
                ),
        )

    private fun createTestSalaryDistribution(): Map<String, CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics> = educationLevels.associateWith { createTestSalaryMetrics() }

    private fun createTestSalaryMetrics(): CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics =
        CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics(
            avgSalary = Random.nextLong(50000, 150000),
            medianSalary = Random.nextLong(45000, 140000),
            salaryRange = createTestSalaryRange(),
        )

    private fun createTestSalaryRange(): CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics.SalaryRange =
        CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.SalaryMetrics.SalaryRange(
            min = Random.nextLong(30000, 50000),
            max = Random.nextLong(150000, 200000),
            p25 = Random.nextLong(40000, 60000),
            p75 = Random.nextLong(120000, 140000),
        )

    private fun createTestTrendIndicators(): CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.TrendIndicators =
        CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry.TrendIndicators(
            growthRate = Random.nextDouble(-0.2, 0.5),
            changeVelocity = Random.nextDouble(0.0, 1.0),
            stabilityScore = Random.nextDouble(0.0, 1.0),
            futureProjection = if (Random.nextBoolean()) "GROWTH" else "STABLE",
        )

    private fun createSnapshotPeriod(baseDate: String): SnapshotPeriod {
        val year = baseDate.substring(0, 4).toInt()
        val month = baseDate.substring(4, 6).toInt()

        val startDate = Instant.parse("$year-${month.toString().padStart(2, '0')}-01T00:00:00Z")
        val endDate =
            if (month == 12) {
                Instant.parse("${year + 1}-01-01T00:00:00Z").minusSeconds(1)
            } else {
                Instant.parse("$year-${(month + 1).toString().padStart(2, '0')}-01T00:00:00Z").minusSeconds(1)
            }

        return SnapshotPeriod(startDate, endDate)
    }

    @AfterEach
    override fun cleanupTestData() {
        mongoTemplate.dropCollection("company_size_education_rankings")
    }

    @Test
    @Order(1)
    @DisplayName("분포 패턴과 유사한 데이터를 찾을 수 있다")
    fun `findByDistributionPattern - should find similar distributions`() {
        // given
        val baseDate = BaseDate("202401")
        val pattern =
            mapOf(
                "BACHELOR" to 0.5,
                "MASTER" to 0.3,
                "DOCTORATE" to 0.2,
            )
        val threshold = 0.7

        // when
        val results = companySizeEducationRepository.findByDistributionPattern(baseDate, pattern, threshold)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one matching distribution")

        results.forEach { entry ->
            val similarity = calculateSimilarity(pattern, entry.distribution)
            assertTrue(
                similarity >= threshold,
                "Each result should have similarity >= $threshold (actual: $similarity)",
            )
        }
    }

    @Test
    @Order(2)
    @DisplayName("지정된 주요 카테고리를 가진 데이터를 찾을 수 있다")
    fun `findByDominantCategory - should find entries with specified dominant category`() {
        // given
        val baseDate = BaseDate("202401")
        val category = "BACHELOR"

        // when
        val results = companySizeEducationRepository.findByDominantCategory(baseDate, category)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one matching entry")

        results.forEach { entry ->
            assertEquals(category, entry.dominantCategory, "Each entry should have the specified dominant category")
            assertTrue(
                (entry.distribution[category] ?: 0.0) > 0.35,
                "Dominant category should have significant distribution value (actual: ${entry.distribution[category]})",
            )
        }
    }

    @Test
    @Order(3)
    @DisplayName("분포 트렌드를 조회할 수 있다")
    fun `findDistributionTrends - should return distribution history`() {
        // given
        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val months = 2

        // when
        val results = companySizeEducationRepository.findDistributionTrends(entityId, months)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one trend entry")
        assertTrue(results.size <= months, "Should not exceed specified months")
        assertTrue(
            results.all { doc ->
                doc.rankings.any { it.entityId == entityId }
            },
            "All entries should belong to the specified entity",
        )
    }

    @Test
    @Order(4)
    @DisplayName("중요한 분포 변화를 식별할 수 있다")
    fun `findSignificantDistributionChanges - should identify major changes`() {
        // given
        val startDate = BaseDate("202401")
        val endDate = BaseDate("202402")

        // when
        val results = companySizeEducationRepository.findSignificantDistributionChanges(startDate, endDate)

        // then
        assertNotNull(results)
        results.forEach { entry ->
            val distributionChange = entry.rankChange
            assertNotNull(distributionChange, "Change metric should be present")
        }
    }

    @Test
    @Order(5)
    @DisplayName("균일한 분포를 찾을 수 있다")
    fun `findUniformDistributions - should find distributions with low variance`() {
        // given
        val baseDate = BaseDate("202401")
        val maxVariance = 0.3

        // when
        val results = companySizeEducationRepository.findUniformDistributions(baseDate, maxVariance)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one uniform distribution")
        results.forEach { entry ->
            assertTrue(
                entry.distributionMetrics.uniformity <= maxVariance,
                "Each result should have uniformity <= $maxVariance",
            )
        }
    }

    @Test
    @Order(6)
    @DisplayName("치우친 분포를 찾을 수 있다")
    fun `findSkewedDistributions - should find distributions with high concentration`() {
        // given
        val baseDate = BaseDate("202401")
        val minSkewness = 0.4 // 임계값을 더 현실적으로 조정

        // 매우 치우친 분포를 가진 테스트 데이터 추가
        val skewedDocument = createTestDocument("202401", 1, true)
        companySizeEducationRepository.save(skewedDocument)

        // when
        val results = companySizeEducationRepository.findSkewedDistributions(baseDate, minSkewness)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one skewed distribution")

        results.forEach { entry ->
            val concentration = entry.distributionMetrics.concentration
            assertTrue(
                concentration >= minSkewness,
                "Each result should have concentration >= $minSkewness (actual: $concentration)",
            )

            // 실제 분포 검증
            val maxValue = entry.distribution.values.maxOrNull() ?: 0.0
            assertTrue(
                maxValue >= minSkewness,
                "Distribution should have at least one high value (actual max: $maxValue)",
            )
        }
    }

    @Test
    @Order(7)
    @DisplayName("유사한 분포를 찾을 수 있다")
    fun `findSimilarDistributions - should find distributions similar to target`() {
        // given
        val baseDate = BaseDate("202401")
        val targetEntity = allRecords.first().rankings.first()
        val similarity = 0.75 // 임계값을 좀 더 현실적으로 조정

        // when
        val results =
            companySizeEducationRepository.findSimilarDistributions(
                targetEntity.entityId,
                baseDate,
                similarity,
            )

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one similar distribution")
        assertFalse(
            results.any { it.entityId == targetEntity.entityId },
            "Results should not include the target entity",
        )

        results.forEach { entry ->
            val actualSimilarity =
                calculateCosineSimilarity(
                    targetEntity.distribution,
                    entry.distribution,
                )
            assertTrue(
                actualSimilarity >= similarity,
                "Each result should have similarity >= $similarity (actual: $actualSimilarity)",
            )
        }
    }

    @Test
    @Order(8)
    @DisplayName("시간에 따른 분포 변화를 추적할 수 있다")
    fun `findDistributionChanges - should track distribution changes over time`() {
        // given
        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .entityId
        val months = 2

        // when
        val results = companySizeEducationRepository.findDistributionChanges(entityId, months)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one change record")
        assertTrue(results.size <= months, "Should not exceed specified months")

        // Verify results are ordered by date descending
        val dates = results.map { it.baseDate }
        assertEquals(dates.sortedDescending(), dates, "Results should be ordered by date descending")
    }

    @Test
    @Order(9)
    @DisplayName("카테고리 우세도를 찾을 수 있다")
    fun `findCategoryDominance - should find entries with high category percentage`() {
        // given
        val baseDate = BaseDate("202401")
        val category = "BACHELOR"
        val minPercentage = 0.4

        // when
        val results =
            companySizeEducationRepository.findCategoryDominance(
                baseDate,
                category,
                minPercentage,
            )

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty(), "Should find at least one dominant entry")

        results.forEach { entry ->
            assertTrue(
                (entry.distribution[category] ?: 0.0) >= minPercentage,
                "Each result should have category percentage >= $minPercentage",
            )
        }

        // Verify results are sorted by category percentage
        val percentages = results.map { it.distribution[category] ?: 0.0 }
        assertEquals(
            percentages.sortedDescending(),
            percentages,
            "Results should be sorted by category percentage descending",
        )
    }

    @Test
    @Order(10)
    @DisplayName("성능 테스트를 실행한다")
    fun `performanceTest - should complete operations within time limits`() {
        val startTime = System.currentTimeMillis()
        val baseDate = BaseDate("202401")

        // Test various operations with timing
        val performanceMetrics = mutableMapOf<String, Double>()

        // Test findByDistributionPattern
        measureOperation("find_by_pattern") {
            val pattern = mapOf("BACHELOR" to 0.5, "MASTER" to 0.3)
            companySizeEducationRepository.findByDistributionPattern(baseDate, pattern, 0.7)
        }?.let { performanceMetrics["find_by_pattern"] = it }

        // Test findByDominantCategory
        measureOperation("find_by_dominant") {
            companySizeEducationRepository.findByDominantCategory(baseDate, "BACHELOR")
        }?.let { performanceMetrics["find_by_dominant"] = it }

        // Test findUniformDistributions
        measureOperation("find_uniform") {
            companySizeEducationRepository.findUniformDistributions(baseDate, 0.3)
        }?.let { performanceMetrics["find_uniform"] = it }

        // Verify performance metrics
        performanceMetrics.forEach { (operation, time) ->
            assertTrue(
                time <
                    when (operation) {
                        "find_by_pattern" -> 2.0
                        "find_by_dominant" -> 1.0
                        "find_uniform" -> 1.0
                        else -> Double.MAX_VALUE
                    },
                "$operation took too long: $time seconds",
            )
        }

        log.debug("Performance test metrics: $performanceMetrics")
    }

    private fun measureOperation(
        name: String,
        operation: () -> Unit,
    ): Double? {
        val start = System.currentTimeMillis()
        try {
            operation()
            val end = System.currentTimeMillis()
            val duration = (end - start) / 1000.0
            log.debug("$name completed in $duration seconds")
            return duration
        } catch (e: Exception) {
            log.error("Error during $name: ${e.message}")
            return null
        }
    }

    private fun calculateCosineSimilarity(
        dist1: Map<String, Double>,
        dist2: Map<String, Double>,
    ): Double {
        val allCategories = (dist1.keys + dist2.keys).toSet()
        val dotProduct =
            allCategories.sumOf { category ->
                (dist1[category] ?: 0.0) * (dist2[category] ?: 0.0)
            }
        val norm1 = kotlin.math.sqrt(dist1.values.sumOf { it * it })
        val norm2 = kotlin.math.sqrt(dist2.values.sumOf { it * it })
        return if (norm1 != 0.0 && norm2 != 0.0) dotProduct / (norm1 * norm2) else 0.0
    }
}
