package com.wildrew.jobstat.statistics_read.repository

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.wildrew.jobstat.statistics_read.rankings.document.IndustrySkillRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.repository.IndustrySkillRankingsRepositoryImpl
import com.wildrew.jobstat.statistics_read.utils.config.BatchOperationTestSupport
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
            documentId = "doc_rel_${primaryEntityId}_$rank",
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
                VolatilityMetrics(
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
        val baseDate = BaseDate("202401")
        val primaryEntityId =
            allRecords
                .first()
                .rankings
                .first()
                .primaryEntityId

        val result = industrySkillRankingsRepository.findByPrimaryEntityId(primaryEntityId, baseDate)

        assertNotNull(result)
        assertEquals(primaryEntityId, result?.primaryEntityId)
        assertTrue(result?.relatedRankings?.isNotEmpty() ?: false)
    }

    @Test
    @Order(4)
    @DisplayName("강한 관계를 가진 데이터를 찾을 수 있다")
    fun `findStrongRelationships - should find relationships with scores above threshold`() {
        val baseDate = BaseDate("202401")
        val minScore = 0.8

        val results = industrySkillRankingsRepository.findStrongRelationships(baseDate, minScore)

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
        val baseDate = BaseDate("202401")
        val limit = 5

        val results = industrySkillRankingsRepository.findStrongestPairs(baseDate, limit)

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
        val baseDate = BaseDate("202401")
        val primaryEntity1 = allRecords[0].rankings.first().primaryEntityId
        val primaryEntity2 = allRecords[1].rankings.first().primaryEntityId

        val results =
            industrySkillRankingsRepository.findCommonRelationships(
                primaryEntity1,
                primaryEntity2,
                baseDate,
            )

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
