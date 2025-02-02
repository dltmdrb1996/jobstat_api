package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.statistics.rankings.document.SkillGrowthRankingsDocument
import com.example.jobstat.statistics.rankings.repository.SkillGrowthRankingsRepositoryImpl
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

@TestMethodOrder(OrderAnnotation::class)
class SimpleRankingRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var skillGrowthRankingsRepository: SkillGrowthRankingsRepositoryImpl

    // 저장된 테스트 도큐먼트를 추후 삭제하기 위한 리스트
    private val allDocuments = mutableListOf<SkillGrowthRankingsDocument>()

    @BeforeEach
    override fun setup() {
        cleanupTestData() // 이전 테스트 데이터 삭제

        val baseDate = "202401"

        // -----------------------------
        // findByValueRange 테스트용 도큐먼트
        // -----------------------------
        // doc1: 두 개 엔트리, score 50.0, 75.0
        val doc1 =
            createTestDocument(
                baseDate = baseDate,
                page = 1,
                rankingEntries =
                    listOf(
                        createTestRankingEntry(rank = 1, score = 50.0, rankChange = 5),
                        createTestRankingEntry(rank = 2, score = 75.0, rankChange = 8),
                    ),
            )
        // -----------------------------
        // findRisingStars 테스트용 도큐먼트
        // -----------------------------
        // doc2: 두 개 엔트리, rankChange 12, 15 (조건: minRankImprovement = 10)
        val doc2 =
            createTestDocument(
                baseDate = baseDate,
                page = 2,
                rankingEntries =
                    listOf(
                        createTestRankingEntry(rank = 3, score = 60.0, rankChange = 12),
                        createTestRankingEntry(rank = 4, score = 65.0, rankChange = 15),
                    ),
            )
        // -----------------------------
        // findByEntityIdAndBaseDate 테스트용 도큐먼트
        // -----------------------------
        // doc3: 두 개 엔트리, 그 중 하나의 entityId를 555L로 지정
        val doc3 =
            createTestDocument(
                baseDate = baseDate,
                page = 3,
                rankingEntries =
                    listOf(
                        createTestRankingEntry(rank = 5, score = 80.0, rankChange = 3, entityId = 555L),
                        createTestRankingEntry(rank = 6, score = 85.0, rankChange = 4, entityId = 556L),
                    ),
            )

        listOf(doc1, doc2, doc3).forEach {
            allDocuments.add(it)
            skillGrowthRankingsRepository.save(it)
        }
    }

    override fun cleanupTestData() {
        skillGrowthRankingsRepository.deleteAll()
    }

    @AfterEach
    override fun tearDown() {
        cleanupTestData()
    }

    /**
     * findByValueRange 테스트
     * baseDate "202401"에서 score가 60.0 ~ 80.0 사이인 랭킹 엔트리를 조회하고,
     * 내림차순 정렬 결과(예: 80.0, 75.0, 65.0, 60.0)를 검증한다.
     */
    @Test
    @Order(1)
    fun testFindByValueRange() {
        // "doc1": scores 50.0, 75.0
        // "doc2": scores 60.0, 65.0
        // "doc3": scores 80.0, 85.0
        // 조건(60.0 <= score <= 80.0)에 해당하는 엔트리: 75.0, 60.0, 65.0, 80.0
        // 단, findByValueRange 메서드는 score 내림차순 정렬하여 반환
        val results = skillGrowthRankingsRepository.findByValueRange("202401", 60.0, 80.0)
        assertNotNull(results)
        // 예상: 80.0, 75.0, 65.0, 60.0 (총 4건)
        assertEquals(4, results.size)
        val sortedScores = results.map { it.score }
        assertEquals(listOf(80.0, 75.0, 65.0, 60.0), sortedScores)
    }

    /**
     * findRisingStars 테스트
     * 최근 2개의 도큐먼트(페이지 2, 3 등)에서 rankChange가 최소 10 이상인 엔트리를 조회한다.
     * 조건에 부합하는 것은 doc2의 두 엔트리 (rankChange 12, 15)이며, 내림차순 정렬되어야 한다.
     */
    @Test
    @Order(2)
    fun testFindRisingStars() {
        // minRankImprovement = 10, 최근 2개의 도큐먼트 기준
        val results = skillGrowthRankingsRepository.findRisingStars(2, 10)
        assertNotNull(results)
        // doc2의 두 엔트리(rankChange 12, 15)만 해당하므로, 결과는 2건이어야 함
        assertEquals(2, results.size)
        // 내림차순 정렬: 15, 12
        val rankChanges = results.map { it.rankChange }
        assertEquals(listOf(15, 12), rankChanges)
    }

    /**
     * findByEntityIdAndBaseDate 테스트
     * baseDate "202401"에서 entityId가 555L인 랭킹 엔트리를 포함하는 도큐먼트를 조회한다.
     */
    @Test
    @Order(3)
    fun testFindByEntityIdAndBaseDate() {
        val resultDoc = skillGrowthRankingsRepository.findByEntityIdAndBaseDate(555L, "202401")
        assertNotNull(resultDoc)
        resultDoc?.let {
            val entry = it.rankings.find { ranking -> ranking.entityId == 555L }
            assertNotNull(entry)
            assertEquals("202401", it.baseDate)
        }
    }

    // ─── HELPER METHODS ──────────────────────────────────────────────────────────────

    private fun createTestDocument(
        baseDate: String,
        page: Int,
        rankingEntries: List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry>,
    ): SkillGrowthRankingsDocument =
        SkillGrowthRankingsDocument(
            baseDate = baseDate,
            page = page,
            period = createSnapshotPeriod(baseDate),
            metrics = createTestMetrics(),
            rankings = rankingEntries,
        )

    private fun createTestRankingEntry(
        rank: Int,
        score: Double,
        rankChange: Int?,
        entityId: Long = rank.toLong(),
    ): SkillGrowthRankingsDocument.SkillGrowthRankingEntry =
        SkillGrowthRankingsDocument.SkillGrowthRankingEntry(
            documentId = "doc_$rank",
            entityId = entityId,
            name = "Skill_$rank",
            rank = rank,
            previousRank = rank + Random.nextInt(-5, 6),
            rankChange = rankChange,
            score = score,
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

    private fun createTestMetrics(): SkillGrowthRankingsDocument.SkillGrowthMetrics =
        SkillGrowthRankingsDocument.SkillGrowthMetrics(
            totalCount = 1000,
            rankedCount = 1000,
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

    private fun createSnapshotPeriod(baseDate: String): SnapshotPeriod {
        val year = baseDate.substring(0, 4).toInt()
        val month = baseDate.substring(4, 6).toInt()
        val startDateTime = LocalDateTime.of(year, month, 1, 0, 0)
        val endDateTime = startDateTime.plusMonths(1).minusSeconds(1)
        return SnapshotPeriod(
            startDateTime.toInstant(ZoneOffset.UTC),
            endDateTime.toInstant(ZoneOffset.UTC),
        )
    }
}
