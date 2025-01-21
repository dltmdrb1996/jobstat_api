package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.state.EntityType
import com.example.jobstat.rankings.model.IndustrySkillRankingsDocument
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RelationshipRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var industrySkillRankingsRepository: IndustrySkillRankingsRepositoryImpl

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<IndustrySkillRankingsDocument>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()

    override fun cleanupTestData() {
        val recordIds = allRecords.mapNotNull { it.id }
        for (batch in recordIds.chunked(batchSize)) {
            industrySkillRankingsRepository.bulkDelete(batch)
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

    private fun createRelatedSkill(rank: Int): IndustrySkillRankingsDocument.IndustrySkillRankingEntry.SkillRank {
        val entityId = Random.nextLong(1000, 9999)
        return IndustrySkillRankingsDocument.IndustrySkillRankingEntry.SkillRank(
            entityId = entityId,
            name = "Skill_$entityId",
            rank = rank,
            score = Random.nextDouble(0.0, 1.0),
            demandLevel = Random.nextDouble(0.0, 1.0),
            growthRate = Random.nextDouble(-0.2, 0.5),
            industrySpecificity = Random.nextDouble(0.0, 1.0),
        )
    }

    private fun createIndustrySkillDocument(
        baseDate: String,
        rank: Int,
        primaryEntityId: Long,
    ): IndustrySkillRankingsDocument {
        val relatedSkillsCount = Random.nextInt(5, 15)
        val relatedRankings = (1..relatedSkillsCount).map { createRelatedSkill(it) }

        return IndustrySkillRankingsDocument(
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            metrics =
                IndustrySkillRankingsDocument.IndustrySkillMetrics(
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
                    industrySkillCorrelation =
                        IndustrySkillRankingsDocument.IndustrySkillMetrics.IndustrySkillCorrelation(
                            crossIndustrySkills =
                                mapOf(
                                    primaryEntityId to
                                        relatedRankings.associate {
                                            it.entityId to Random.nextDouble(0.0, 1.0)
                                        },
                                ),
                            skillTransitionPatterns =
                                listOf(
                                    IndustrySkillRankingsDocument.IndustrySkillMetrics.IndustrySkillCorrelation.SkillTransition(
                                        fromIndustryId = primaryEntityId,
                                        toIndustryId = Random.nextLong(1000, 9999),
                                        commonSkillsCount = Random.nextInt(1, relatedSkillsCount),
                                        transitionScore = Random.nextDouble(0.0, 1.0),
                                    ),
                                ),
                        ),
                ),
            primaryEntityType = EntityType.INDUSTRY,
            relatedEntityType = EntityType.SKILL,
            rankings =
                listOf(
                    IndustrySkillRankingsDocument.IndustrySkillRankingEntry(
                        documentId = "Industry_$primaryEntityId",
                        entityId = primaryEntityId,
                        name = "Industry_$primaryEntityId",
                        rank = rank,
                        previousRank = rank + Random.nextInt(-5, 5),
                        rankChange = Random.nextInt(-5, 5),
                        primaryEntityId = primaryEntityId,
                        primaryEntityName = "Industry_$primaryEntityId",
                        relatedRankings = relatedRankings,
                        totalPostings = Random.nextInt(1000, 10000),
                        industryPenetration = Random.nextDouble(0.0, 1.0),
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
                    createIndustrySkillDocument(baseDate, rank++, Random.nextLong(1000, 9999))
                }
            val result = industrySkillRankingsRepository.bulkInsert(records)
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
    fun testFindByPrimaryEntityId() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val primaryEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .primaryEntityId
        val result = industrySkillRankingsRepository.findByPrimaryEntityId(primaryEntityId, baseDate)

        Assertions.assertNotNull(result)
        result?.let {
            Assertions.assertEquals(primaryEntityId, it.rankings.first().primaryEntityId)
            Assertions.assertEquals(baseDate, it.baseDate)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by primary entity ID execution time: $timeSeconds seconds")
        performanceMetrics["find_by_primary_entity"] = timeSeconds
    }

    @Test
    @Order(3)
    fun testFindTopNRelatedEntities() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val primaryEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .primaryEntityId
        val limit = 5
        val results = industrySkillRankingsRepository.findTopNRelatedEntities(primaryEntityId, baseDate, limit)

        Assertions.assertTrue(results.isNotEmpty())
        results.first().rankings.first().relatedRankings.let { relatedRankings ->
            Assertions.assertTrue(relatedRankings.size <= limit)
            // Verify scores are in descending order
            Assertions.assertEquals(
                relatedRankings.map { it.score },
                relatedRankings.map { it.score }.sortedDescending(),
            )
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find top N related entities execution time: $timeSeconds seconds")
        performanceMetrics["find_top_n_related"] = timeSeconds
    }

    @Test
    @Order(4)
    fun testFindByRelatedEntityId() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val relatedEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .relatedRankings
                .first()
                .entityId
        val results = industrySkillRankingsRepository.findByRelatedEntityId(relatedEntityId, baseDate)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all { doc ->
                doc.rankings
                    .first()
                    .relatedRankings
                    .any { it.entityId == relatedEntityId }
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by related entity ID execution time: $timeSeconds seconds")
        performanceMetrics["find_by_related_entity"] = timeSeconds
    }

    @Test
    @Order(5)
    fun testFindStrongRelationships() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val minScore = 0.8
        val results = industrySkillRankingsRepository.findStrongRelationships(baseDate, minScore)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all { doc ->
                doc.rankings
                    .first()
                    .relatedRankings
                    .any { it.score >= minScore }
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find strong relationships execution time: $timeSeconds seconds")
        performanceMetrics["find_strong_relationships"] = timeSeconds
    }

    @Test
    @Order(6)
    fun testFindGrowingRelationships() {
        startTime = System.currentTimeMillis()

        val startDate = "202401"
        val endDate = "202402"
        val limit = 5
        val results = industrySkillRankingsRepository.findGrowingRelationships(startDate, endDate, limit)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= limit)

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find growing relationships execution time: $timeSeconds seconds")
        performanceMetrics["find_growing_relationships"] = timeSeconds
    }

    @Test
    @Order(7)
    fun testFindCommonRelationships() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val primaryEntity1 = allRecords[0].rankings.first().primaryEntityId
        val primaryEntity2 = allRecords[1].rankings.first().primaryEntityId
        val results =
            industrySkillRankingsRepository.findCommonRelationships(
                primaryEntity1,
                primaryEntity2,
                baseDate,
            )

        // Results should contain skills that are common between both industries
        Assertions.assertTrue(
            results.all { doc ->
                doc.rankings.first().relatedRankings.any { skill ->
                    skill.industrySpecificity > 0
                }
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find common relationships execution time: $timeSeconds seconds")
        performanceMetrics["find_common_relationships"] = timeSeconds
    }

    @Test
    @Order(8)
    fun testFindStrongestPairs() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val limit = 5
        val results = industrySkillRankingsRepository.findStrongestPairs(baseDate, limit)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= limit)
        // Verify pairs are ordered by score
        results.forEach { doc ->
            val scores =
                doc.rankings
                    .first()
                    .relatedRankings
                    .map { it.score }
            Assertions.assertEquals(scores, scores.sortedDescending())
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find strongest pairs execution time: $timeSeconds seconds")
        performanceMetrics["find_strongest_pairs"] = timeSeconds
    }

    @Test
    @Order(9)
    fun testFindRelationshipTrends() {
        startTime = System.currentTimeMillis()

        val primaryEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .primaryEntityId
        val months = 3
        val results = industrySkillRankingsRepository.findRelationshipTrends(primaryEntityId, months)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= months)
        Assertions.assertTrue(
            results.all { it.rankings.first().primaryEntityId == primaryEntityId },
        )
        // Verify dates are in descending order
        Assertions.assertEquals(
            results.map { it.baseDate },
            results.map { it.baseDate }.sortedDescending(),
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find relationship trends execution time: $timeSeconds seconds")
        performanceMetrics["find_relationship_trends"] = timeSeconds
    }

    @Test
    @Order(10)
    fun testFindEmergingRelationships() {
        startTime = System.currentTimeMillis()

        val months = 3
        val minGrowthRate = 0.2
        val results = industrySkillRankingsRepository.findEmergingRelationships(months, minGrowthRate)

        Assertions.assertTrue(results.isNotEmpty())
        // Verify growth rates meet minimum threshold

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find emerging relationships execution time: $timeSeconds seconds")
        performanceMetrics["find_emerging_relationships"] = timeSeconds
    }

    @Test
    @Order(11)
    fun testFindByEntityIdAndBaseDate() {
        startTime = System.currentTimeMillis()

        val baseDate = "202401"
        val entityId =
            allRecords
                .first()
                .rankings
                .first()
                .primaryEntityId
        val result = industrySkillRankingsRepository.findByEntityIdAndBaseDate(entityId, baseDate)

        Assertions.assertNotNull(result)
        result?.let {
            Assertions.assertEquals(baseDate, it.baseDate)
            // Entity should be either primary or in related rankings
            Assertions.assertTrue(
                it.rankings.first().primaryEntityId == entityId ||
                    it.rankings
                        .first()
                        .relatedRankings
                        .any { skill -> skill.entityId == entityId },
            )
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
            val deletedCount = industrySkillRankingsRepository.bulkDelete(batch)
            totalDeleted += deletedCount
        }

        val remainingRecords = industrySkillRankingsRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }
}
