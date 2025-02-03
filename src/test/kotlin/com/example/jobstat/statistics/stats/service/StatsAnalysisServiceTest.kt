package com.example.jobstat.statistics.stats.service

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.stats.*
import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.stats.model.SkillStatsDocument
import com.example.jobstat.statistics.stats.registry.StatsRepositoryRegistry
import com.example.jobstat.statistics.stats.registry.StatsType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("StatsAnalysisService 테스트")
class StatsAnalysisServiceTest {
    private lateinit var mockRepository: StatsMongoRepository<SkillStatsDocument, String>
    private lateinit var statsRepositoryRegistry: StatsRepositoryRegistry
    private lateinit var statsAnalysisService: StatsAnalysisService
    private val random = Random(12345)

    private val exLevel = listOf("ENTRY", "JUNIOR", "MIDDLE", "SENIOR", "LEAD")
    private val companySize = listOf("SMALL", "MEDIUM", "LARGE", "ENTERPRISE")
    private val jobCategory = listOf(1L to "Backend", 2L to "Frontend", 3L to "DevOps", 4L to "Data Science")
    private val industry =
        listOf(
            1L to "IT",
            2L to "Finance",
            3L to "Healthcare",
            4L to "E-commerce",
        )

    @BeforeEach
    fun setUp() {
        mockRepository = mock()
        statsRepositoryRegistry = mock()

        doReturn(mockRepository)
            .`when`(statsRepositoryRegistry)
            .getRepository<SkillStatsDocument>(any())

        statsAnalysisService = StatsAnalysisServiceImpl(statsRepositoryRegistry)
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

    private fun createSkillStatsDocument(
        entityId: Long,
        baseDate: String,
        name: String = "Skill_$entityId",
        growthRate: Double = random.nextDouble(-20.0, 50.0),
    ): SkillStatsDocument {
        val postingCount = random.nextInt(100, 1000)
        val activePostingCount = (postingCount * random.nextDouble(0.3, 0.9)).toInt()
        val avgSalary = random.nextLong(50000, 150000)

        return SkillStatsDocument(
            id = entityId.toString(),
            entityId = entityId,
            baseDate = baseDate,
            period = createSnapshotPeriod(baseDate),
            name = name,
            stats =
                SkillStatsDocument.SkillStats(
                    postingCount = postingCount,
                    activePostingCount = activePostingCount,
                    avgSalary = avgSalary,
                    growthRate = growthRate,
                    yearOverYearGrowth = random.nextDouble(-10.0, 30.0),
                    monthOverMonthChange = random.nextDouble(-5.0, 15.0),
                    demandTrend = if (growthRate > 0) "INCREASING" else "DECREASING",
                    demandScore = random.nextDouble(0.0, 1.0),
                    companyCount = random.nextInt(10, 100),
                ),
            experienceLevels =
                exLevel
                    .map { level ->
                        SkillStatsDocument.SkillExperienceLevel(
                            range = level,
                            postingCount = random.nextInt(10, 100),
                            avgSalary = avgSalary + random.nextLong(-10000, 20000),
                        )
                    }.toMutableList(),
            companySizeDistribution =
                companySize
                    .map { size ->
                        SkillStatsDocument.CompanySizeDistribution(
                            companySize = size,
                            count = random.nextInt(10, 100),
                            avgSalary = avgSalary + random.nextLong(-5000, 15000),
                        )
                    }.toMutableList(),
            industryDistribution =
                industry
                    .map { (id, name) ->
                        SkillStatsDocument.IndustryDistribution(
                            industryId = id,
                            industryName = name,
                            count = random.nextInt(5, 50),
                            avgSalary = avgSalary + random.nextLong(-8000, 18000),
                        )
                    }.toMutableList(),
            isSoftSkill = random.nextBoolean(),
            isEmergingSkill = growthRate > 30.0,
            relatedJobCategories =
                jobCategory
                    .map { (id, name) ->
                        SkillStatsDocument.RelatedJobCategory(
                            jobCategoryId = id,
                            name = name,
                            postingCount = random.nextInt(5, 50),
                            importanceScore = random.nextDouble(0.0, 1.0),
                            growthRate = growthRate + random.nextDouble(-10.0, 10.0),
                        )
                    }.toMutableList(),
            rankings =
                RankingType.values().associateWith { type ->
                    SkillStatsDocument.SkillRankingInfo(
                        currentRank = random.nextInt(1, 100),
                        previousRank = random.nextInt(1, 100),
                        rankChange = random.nextInt(-10, 10),
                        percentile = random.nextDouble(0.0, 100.0),
                        rankingScore =
                            when (type) {
                                // 채용공고 수 관련 랭킹
                                RankingType.SKILL_POSTING_COUNT,
                                RankingType.JOB_CATEGORY_POSTING_COUNT,
                                RankingType.CERTIFICATION_POSTING_COUNT,
                                RankingType.LOCATION_POSTING_COUNT,
                                RankingType.COMPANY_SIZE_POSTING_COUNT,
                                RankingType.COMPANY_HIRING_VOLUME,
                                RankingType.BENEFIT_POSTING_COUNT,
                                ->
                                    PostingCountScore(
                                        value = postingCount.toDouble(),
                                        totalPostings = postingCount,
                                        activePostings = activePostingCount,
                                    )

                                // 급여 관련 랭킹
                                RankingType.SKILL_SALARY,
                                RankingType.JOB_CATEGORY_SALARY,
                                RankingType.INDUSTRY_SALARY,
                                RankingType.CERTIFICATION_SALARY,
                                RankingType.LOCATION_SALARY,
                                RankingType.COMPANY_SIZE_SALARY,
                                RankingType.COMPANY_SALARY,
                                RankingType.EDUCATION_SALARY,
                                ->
                                    SalaryScore(
                                        value = avgSalary.toDouble(),
                                        avgSalary = avgSalary,
                                        medianSalary = avgSalary + random.nextLong(-5000, 5000),
                                    )

                                // 성장률 관련 랭킹
                                RankingType.SKILL_GROWTH,
                                RankingType.JOB_CATEGORY_GROWTH,
                                RankingType.INDUSTRY_GROWTH,
                                RankingType.CERTIFICATION_GROWTH,
                                RankingType.LOCATION_GROWTH,
                                RankingType.COMPANY_GROWTH,
                                ->
                                    GrowthScore(
                                        value = growthRate,
                                        growthRate = growthRate,
                                        consistencyScore = random.nextDouble(0.0, 1.0),
                                    )

                                // 수요도/선호도 관련 랭킹
                                RankingType.SKILL_COMPETITION_RATE,
                                RankingType.JOB_CATEGORY_SKILL,
                                RankingType.INDUSTRY_SKILL,
                                RankingType.JOB_CATEGORY_APPLICATION_RATE,
                                RankingType.COMPANY_SIZE_SKILL_DEMAND,
                                ->
                                    DemandScore(
                                        value = random.nextDouble(0.0, 100.0),
                                        applicationRate = random.nextDouble(0.0, 1.0),
                                        marketDemand = random.nextDouble(0.0, 1.0),
                                    )

                                // 회사 관련 특수 랭킹
                                RankingType.COMPANY_RETENTION_RATE ->
                                    CompanyWorkLifeBalanceScore(
                                        value = random.nextDouble(0.0, 100.0),
                                        satisfactionRate = random.nextDouble(0.0, 1.0),
                                        workHoursFlexibility = random.nextDouble(0.0, 1.0),
                                        benefitsSatisfaction = random.nextDouble(0.0, 1.0),
                                        remoteWorkScore = random.nextDouble(0.0, 1.0),
                                    )

                                // 복리후생 관련 랭킹
                                RankingType.COMPANY_SIZE_BENEFIT,
                                RankingType.COMPANY_BENEFIT_COUNT,
                                ->
                                    CompanyWorkLifeBalanceScore(
                                        value = random.nextDouble(0.0, 100.0),
                                        satisfactionRate = random.nextDouble(0.0, 1.0),
                                        workHoursFlexibility = random.nextDouble(0.0, 1.0),
                                        benefitsSatisfaction = random.nextDouble(0.0, 1.0),
                                        remoteWorkScore = random.nextDouble(0.0, 1.0),
                                    )

                                // 교육/학력 관련 랭킹
                                RankingType.COMPANY_SIZE_EDUCATION ->
                                    EntryLevelFriendlinessScore(
                                        value = random.nextDouble(0.0, 100.0),
                                        entryLevelHiringRate = random.nextDouble(0.0, 1.0),
                                        trainingProgramQuality = random.nextDouble(0.0, 1.0),
                                        mentorshipAvailability = random.nextDouble(0.0, 1.0),
                                    )
                            },
                    )
                },
        )
    }

    @Nested
    @DisplayName("엔티티 ID와 기준일자로 통계 조회")
    inner class FindStatsByEntityIdAndBaseDate {
        @Test
        @DisplayName("정상적으로 통계를 조회할 수 있다")
        fun findStatsSuccessfully() {
            // given
            val statsType = StatsType.SKILL
            val baseDate = BaseDate("202501")
            val entityId = 1L
            val mockStats = createSkillStatsDocument(entityId, baseDate.toString())

            doReturn(mockStats)
                .`when`(mockRepository)
                .findByEntityIdAndBaseDate(eq(entityId), eq(baseDate))

            // when
            val result =
                statsAnalysisService.findStatsByEntityIdAndBaseDate<SkillStatsDocument>(
                    statsType,
                    baseDate,
                    entityId,
                )

            // then
            assertNotNull(result)
            assertEquals(entityId, result.entityId)
            assertEquals(baseDate.toString(), result.baseDate)
            verify(mockRepository).findByEntityIdAndBaseDate(eq(entityId), eq(baseDate))
        }
    }

    @Nested
    @DisplayName("여러 엔티티 ID로 통계 조회")
    inner class FindStatsByEntityIds {
        @Test
        @DisplayName("정상적으로 여러 엔티티의 통계를 조회할 수 있다")
        fun findStatsByEntityIdsSuccessfully() {
            // given
            val statsType = StatsType.SKILL
            val baseDate = BaseDate("202501")
            val entityIds = listOf(1L, 2L, 3L)
            val mockStats =
                entityIds.map {
                    createSkillStatsDocument(it, baseDate.toString())
                }

            doReturn(mockStats)
                .`when`(mockRepository)
                .findByBaseDateAndEntityIds(eq(baseDate), eq(entityIds))

            // when
            val result =
                statsAnalysisService.findStatsByEntityIds<SkillStatsDocument>(
                    statsType,
                    baseDate,
                    entityIds,
                )

            // then
            assertEquals(entityIds.size, result.size)
            assertTrue(result.map { it.entityId }.containsAll(entityIds))
            verify(mockRepository).findByBaseDateAndEntityIds(eq(baseDate), eq(entityIds))
        }

        @Test
        @DisplayName("엔티티 ID 리스트가 비어있을 경우 빈 리스트를 반환한다")
        fun returnEmptyListWhenEntityIdsIsEmpty() {
            // given
            val statsType = StatsType.SKILL
            val baseDate = BaseDate("202501")
            val entityIds = emptyList<Long>()

            // when
            val result =
                statsAnalysisService.findStatsByEntityIds<SkillStatsDocument>(
                    statsType,
                    baseDate,
                    entityIds,
                )

            // then
            assertTrue(result.isEmpty())
            verify(mockRepository, never()).findByBaseDateAndEntityIds(any(), any())
        }
    }

    @Nested
    @DisplayName("최신 통계 조회")
    inner class FindLatestStats {
        @Test
        @DisplayName("정상적으로 최신 통계를 조회할 수 있다")
        fun findLatestStatsSuccessfully() {
            // given
            val statsType = StatsType.SKILL
            val entityId = 1L
            val mockStats = createSkillStatsDocument(entityId, "202412")

            doReturn(mockStats)
                .`when`(mockRepository)
                .findLatestStatsByEntityId(eq(entityId))

            // when
            val result =
                statsAnalysisService.findLatestStats<SkillStatsDocument>(
                    statsType,
                    entityId,
                )

            // then
            assertNotNull(result)
            assertEquals(entityId, result.entityId)
            verify(mockRepository).findLatestStatsByEntityId(eq(entityId))
        }
    }

    @Nested
    @DisplayName("엔티티 ID로 모든 통계 조회")
    inner class FindStatsByEntityId {
        @Test
        @DisplayName("정상적으로 엔티티의 모든 통계를 조회할 수 있다")
        fun findStatsByEntityIdSuccessfully() {
            // given
            val statsType = StatsType.SKILL
            val entityId = 1L
            val mockStats =
                listOf(
                    createSkillStatsDocument(entityId, "202401"),
                    createSkillStatsDocument(entityId, "202402"),
                )

            doReturn(mockStats)
                .`when`(mockRepository)
                .findByEntityId(eq(entityId))

            // when
            val result =
                statsAnalysisService.findStatsByEntityId<SkillStatsDocument>(
                    statsType,
                    entityId,
                )

            // then
            assertNotNull(result)
            assertTrue(result!!.all { it.entityId == entityId })
            assertEquals(2, result.size)
            verify(mockRepository).findByEntityId(eq(entityId))
        }
    }
}
