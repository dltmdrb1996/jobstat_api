package com.example.jobstat.core.base.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.example.jobstat.statistics_read.core.core_model.BaseDate
import com.example.jobstat.statistics_read.core.core_model.EntityType
import com.example.jobstat.statistics_read.rankings.document.IndustrySkillRankingsDocument
import com.example.jobstat.statistics_read.rankings.repository.IndustrySkillRankingsRepositoryImpl
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("RelationshipRankingRepository 통합 테스트")
class RelationshipRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var industrySkillRankingsRepository: IndustrySkillRankingsRepositoryImpl

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<IndustrySkillRankingsDocument>()

    private fun createBaseDateString(
        year: Int,
        month: Int,
    ): String = "$year${month.toString().padStart(2, '0')}"

    private fun createTestDocument(
        baseDate: String,
        page: Int,
        primaryEntityId: Long,
    ): IndustrySkillRankingsDocument {
        val relatedEntitiesCount = Random.nextInt(5, 15)
        val relatedRankings = (1..relatedEntitiesCount).map { createRelatedEntity(it) }

        return IndustrySkillRankingsDocument(
            id = null,
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            metrics = createTestMetrics(),
            primaryEntityType = EntityType.INDUSTRY,
            relatedEntityType = EntityType.SKILL,
            rankings = listOf(createTestRankingEntry(primaryEntityId, page, relatedRankings)),
            page = page,
        )
    }

    private fun createRelatedEntity(rank: Int): IndustrySkillRankingsDocument.IndustrySkillRankingEntry.SkillRank {
        val entityId = Random.nextLong(1000, 9999)
        return IndustrySkillRankingsDocument.IndustrySkillRankingEntry.SkillRank(
            entityId = entityId,
            name = "Skill_$entityId",
            rank = rank,
            score = Random.nextDouble(0.8, 1.0),
            demandLevel = Random.nextDouble(0.0, 1.0),
            growthRate = Random.nextDouble(-0.2, 0.5),
            industrySpecificity = Random.nextDouble(0.0, 1.0),
        )
    }

    private fun createTestRankingEntry(
        primaryEntityId: Long,
        rank: Int,
        relatedRankings: List<IndustrySkillRankingsDocument.IndustrySkillRankingEntry.SkillRank>,
    ): IndustrySkillRankingsDocument.IndustrySkillRankingEntry =
        IndustrySkillRankingsDocument.IndustrySkillRankingEntry(
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
        )

    private fun createTestMetrics(): IndustrySkillRankingsDocument.IndustrySkillMetrics =
        IndustrySkillRankingsDocument.IndustrySkillMetrics(
            totalCount = totalRecords,
            rankedCount = totalRecords,
            newEntries = 0,
            droppedEntries = 0,
            volatilityMetrics =
            com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics(
                avgRankChange = Random.nextDouble(0.0, 5.0),
                rankChangeStdDev = Random.nextDouble(0.0, 2.0),
                volatilityTrend = "STABLE",
            ),
            industrySkillCorrelation =
                IndustrySkillRankingsDocument.IndustrySkillMetrics.IndustrySkillCorrelation(
                    crossIndustrySkills = emptyMap(),
                    skillTransitionPatterns = emptyList(),
                ),
        )

    @BeforeEach
    override fun setup() {
        val baseDate = createBaseDateString(2024, 1)
        val totalPages = (totalRecords + batchSize - 1) / batchSize

        for (page in 1..totalPages) {
            val primaryEntityId = Random.nextLong(1000, 9999)
            val document = createTestDocument(baseDate, page, primaryEntityId)
            allRecords.add(document)
            industrySkillRankingsRepository.save(document)
        }
    }

    @Test
    @Order(1)
    @DisplayName("주요 엔티티 ID로 데이터를 조회할 수 있다")
    fun `findByPrimaryEntityId - should return correct entry for primary entity id`() {
        // given
        val baseDate = BaseDate("202401")
        val primaryEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .primaryEntityId

        // when
        val result = industrySkillRankingsRepository.findByPrimaryEntityId(primaryEntityId, baseDate)

        // then
        assertNotNull(result)
        assertEquals(primaryEntityId, result?.primaryEntityId)
        assertTrue(result?.relatedRankings?.isNotEmpty() ?: false)
    }

//    @Test
//    @Order(2)
//    fun `findTopNRelatedEntities - should return limited number of related entities sorted by score`() {
//        // given
//        val baseDate = BaseDate("202401")
//        val primaryEntityId = allRecords.first().rankings.first().primaryEntityId
//        val limit = 5
//
//        // when
//        val results = industrySkillRankingsRepository.findTopNRelatedEntities(primaryEntityId, baseDate, limit)
//
//        // log for debugging
//        println("Base date used: ${baseDate}")
//        println("Primary entity ID: $primaryEntityId")
//        println("Results size: ${results.size}")
//
//        // then
//        assertNotNull(results, "Results should not be null")
//        assertTrue(results.isNotEmpty(), "Should return at least one related entity")
//        assertTrue(results.size <= limit, "Results should not exceed limit")
//
//        if (results.isNotEmpty()) {
//            val scores = results.map { it.score }
//            assertEquals(
//                scores.sortedByDescending { it },
//                scores,
//                "Results should be sorted by score in descending order"
//            )
//        }
//    }
//
//    @Test
//    @Order(3)
//    fun `findByRelatedEntityId - should find all entries containing related entity id`() {
//        // given
//        val baseDate = BaseDate("202401")
//        val relatedEntity = allRecords.first().rankings.first().relatedRankings.first()
//
//        // when
//        val results = industrySkillRankingsRepository.findByRelatedEntityId(relatedEntity.entityId, baseDate)
//
//        // log for debugging
//        println("Base date used: ${baseDate}")
//        println("Related entity ID: ${relatedEntity.entityId}")
//        println("Results size: ${results.size}")
//
//        // then
//        assertNotNull(results, "Results should not be null")
//        assertTrue(results.isNotEmpty(), "Should find at least one entry")
//
//        results.forEach { entry ->
//            assertTrue(
//                entry.relatedRankings.any { rank -> rank.entityId == relatedEntity.entityId },
//                "Each entry should contain the related entity ID"
//            )
//        }
//    }

    @Test
    @Order(4)
    @DisplayName("강한 관계를 가진 데이터를 찾을 수 있다")
    fun `findStrongRelationships - should find relationships with scores above threshold`() {
        // given
        val baseDate = BaseDate("202401")
        val minScore = 0.8

        // when
        val results = industrySkillRankingsRepository.findStrongRelationships(baseDate, minScore)

        // then
        assertNotNull(results)
        assertTrue(
            results.all { entry ->
                entry.relatedRankings.any { it.score >= minScore }
            },
        )
    }

    @Test
    @Order(5)
    @DisplayName("가장 강한 관계를 가진 상위 쌍을 찾을 수 있다")
    fun `findStrongestPairs - should return limited number of pairs with highest scores`() {
        // given
        val baseDate = BaseDate("202401")
        val limit = 5

        // when
        val results = industrySkillRankingsRepository.findStrongestPairs(baseDate, limit)

        // then
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.size <= limit)
        val maxScores = results.map { entry -> entry.relatedRankings.maxOf { it.score } }
        assertEquals(maxScores, maxScores.sortedByDescending { it })
    }

    @Test
    @Order(6)
    @DisplayName("공통 관계를 가진 데이터를 찾을 수 있다")
    fun `findCommonRelationships - should find shared relationships between entities`() {
        // given
        val baseDate = BaseDate("202401")
        val primaryEntity1 = allRecords[0].rankings.first().primaryEntityId
        val primaryEntity2 = allRecords[1].rankings.first().primaryEntityId

        // when
        val results =
            industrySkillRankingsRepository.findCommonRelationships(
                primaryEntity1,
                primaryEntity2,
                baseDate,
            )

        // then
        assertNotNull(results)
        if (results.isNotEmpty()) {
            val scores = results.map { it.score }
            assertEquals(scores, scores.sortedByDescending { it })
        }
    }

    @AfterEach
    override fun cleanupTestData() {
        industrySkillRankingsRepository.deleteAll()
    }

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
}
