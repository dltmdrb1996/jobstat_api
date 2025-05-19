package com.example.jobstat.core.base.repository

import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.core.core_model.BaseDate
import com.example.jobstat.core.core_mongo_base.model.stats.*
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.stats.document.SkillStatsDocument
import com.example.jobstat.statistics_read.stats.repository.SkillStatsRepository
import com.example.jobstat.utils.base.BatchOperationTestSupport
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("StatsMongo 통합 테스트")
class StatsMongoRepositoryIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var skillStatsRepository: SkillStatsRepository

    private val totalRecords = 996
    private val batchSize = 100
    private val allRecords = mutableListOf<SkillStatsDocument>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()
    private val exLevel = listOf("ENTRY", "JUNIOR", "MIDDLE", "SENIOR", "LEAD")
    private val companySize = listOf("SMALL", "MEDIUM", "LARGE", "ENTERPRISE")
    private val jobCategory = listOf(1L to "Backend", 2L to "Frontend", 3L to "DevOps", 4L to "Data Science")
    private val industry =
        listOf(
            1L to "IT",
            2L to "Finance",
            3L to "Healthcare",
            4L to "E-commerce",
            5L to "Manufacturing",
            6L to "Education",
            7L to "Retail",
            8L to "Telecommunications",
        )

    override fun cleanupTestData() {
        val recordIds = allRecords.mapNotNull { it.id }
        for (batch in recordIds.chunked(batchSize)) {
            skillStatsRepository.bulkDelete(batch)
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

    private fun createSkillStatsDocument(
        entityId: Long,
        baseDate: String,
        name: String = "Skill_$entityId",
        growthRate: Double = Random.nextDouble(-20.0, 50.0),
    ): SkillStatsDocument {
        val postingCount = Random.nextInt(100, 1000)
        val activePostingCount = (postingCount * Random.nextDouble(0.3, 0.9)).toInt()
        val avgSalary = Random.nextLong(50000, 150000)

        return SkillStatsDocument(
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
                    yearOverYearGrowth = Random.nextDouble(-10.0, 30.0),
                    monthOverMonthChange = Random.nextDouble(-5.0, 15.0),
                    demandTrend = if (growthRate > 0) "INCREASING" else "DECREASING",
                    demandScore = Random.nextDouble(0.0, 1.0),
                    companyCount = Random.nextInt(10, 100),
                ),
            experienceLevels =
                exLevel
                    .map { level ->
                        SkillStatsDocument.SkillExperienceLevel(
                            range = level,
                            postingCount = Random.nextInt(10, 100),
                            avgSalary = avgSalary + Random.nextLong(-10000, 20000),
                        )
                    }.toMutableList(),
            companySizeDistribution =
                companySize
                    .map { size ->
                        SkillStatsDocument.CompanySizeDistribution(
                            companySize = size,
                            count = Random.nextInt(10, 100),
                            avgSalary = avgSalary + Random.nextLong(-5000, 15000),
                        )
                    }.toMutableList(),
            industryDistribution =
                industry
                    .map { (id, name) ->
                        SkillStatsDocument.IndustryDistribution(
                            industryId = id,
                            industryName = name,
                            count = Random.nextInt(5, 50),
                            avgSalary = avgSalary + Random.nextLong(-8000, 18000),
                        )
                    }.toMutableList(),
            isSoftSkill = Random.nextBoolean(),
            isEmergingSkill = growthRate > 30.0,
            relatedJobCategories =
                jobCategory
                    .map { (id, name) ->
                        SkillStatsDocument.RelatedJobCategory(
                            jobCategoryId = id,
                            name = name,
                            postingCount = Random.nextInt(5, 50),
                            importanceScore = Random.nextDouble(0.0, 1.0),
                            growthRate = growthRate + Random.nextDouble(-10.0, 10.0),
                        )
                    }.toMutableList(),
            rankings =
                RankingType.values().associateWith { type ->
                    SkillStatsDocument.SkillRankingInfo(
                        currentRank = Random.nextInt(1, 100),
                        previousRank = Random.nextInt(1, 100),
                        rankChange = Random.nextInt(-10, 10),
                        percentile = Random.nextDouble(0.0, 100.0),
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
                                        medianSalary = avgSalary + Random.nextLong(-5000, 5000),
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
                                        consistencyScore = Random.nextDouble(0.0, 1.0),
                                    )

                                // 수요도/선호도 관련 랭킹
                                RankingType.SKILL_COMPETITION_RATE,
                                RankingType.JOB_CATEGORY_SKILL,
                                RankingType.INDUSTRY_SKILL,
                                RankingType.JOB_CATEGORY_APPLICATION_RATE,
                                RankingType.COMPANY_SIZE_SKILL_DEMAND,
                                ->
                                    DemandScore(
                                        value = Random.nextDouble(0.0, 100.0),
                                        applicationRate = Random.nextDouble(0.0, 1.0),
                                        marketDemand = Random.nextDouble(0.0, 1.0),
                                    )

                                // 회사 관련 특수 랭킹
                                RankingType.COMPANY_RETENTION_RATE ->
                                    CompanyWorkLifeBalanceScore(
                                        value = Random.nextDouble(0.0, 100.0),
                                        satisfactionRate = Random.nextDouble(0.0, 1.0),
                                        workHoursFlexibility = Random.nextDouble(0.0, 1.0),
                                        benefitsSatisfaction = Random.nextDouble(0.0, 1.0),
                                        remoteWorkScore = Random.nextDouble(0.0, 1.0),
                                    )

                                // 복리후생 관련 랭킹
                                RankingType.COMPANY_SIZE_BENEFIT,
                                RankingType.COMPANY_BENEFIT_COUNT,
                                ->
                                    CompanyWorkLifeBalanceScore(
                                        value = Random.nextDouble(0.0, 100.0),
                                        satisfactionRate = Random.nextDouble(0.0, 1.0),
                                        workHoursFlexibility = Random.nextDouble(0.0, 1.0),
                                        benefitsSatisfaction = Random.nextDouble(0.0, 1.0),
                                        remoteWorkScore = Random.nextDouble(0.0, 1.0),
                                    )

                                // 교육/학력 관련 랭킹
                                RankingType.COMPANY_SIZE_EDUCATION ->
                                    EntryLevelFriendlinessScore(
                                        value = Random.nextDouble(0.0, 100.0),
                                        entryLevelHiringRate = Random.nextDouble(0.0, 1.0),
                                        trainingProgramQuality = Random.nextDouble(0.0, 1.0),
                                        mentorshipAvailability = Random.nextDouble(0.0, 1.0),
                                    )
                            },
                    )
                },
        )
    }

    @Test
    @Order(1)
    @DisplayName("대량의 데이터를 삽입할 수 있다")
    fun testBulkInsert() {
        startTime = System.currentTimeMillis()
        allRecords.clear()

        skillStatsRepository.deleteAll()

        val baseDates =
            (1..12).map { month ->
                val monthStr = month.toString().padStart(2, '0')
                "2024$monthStr"
            }

        var totalInserted = 0
        val usedEntityIds = mutableSetOf<Long>()

        for (baseDate in baseDates) {
            val records =
                (1..totalRecords / 12).mapIndexed { idx, it ->
                    createSkillStatsDocument(
                        entityId = idx.toLong(),
                        baseDate = baseDate,
                        name = "Skill_$idx",
                        growthRate = if (idx == 0) 500.0 else Random.nextDouble(-20.0, 50.0),
                    )
                }

            try {
                val result = skillStatsRepository.bulkInsert(records)
                totalInserted += result.size
                allRecords.addAll(result)
                log.debug("Inserted ${result.size} records for $baseDate")
            } catch (e: Exception) {
                log.error("Error inserting records for $baseDate", e)
                throw e
            }
        }

        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

    @Test
    @Order(2)
    @DisplayName("엔티티 ID로 데이터를 조회할 수 있다")
    fun testFindByEntityId() {
        startTime = System.currentTimeMillis()

        val entityId = allRecords.first().entityId
        val results = skillStatsRepository.findByEntityId(entityId)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.all { it.entityId == entityId })
        log.debug("results: ${results.first()}")
        Assertions.assertEquals(
            results.map { it.baseDate },
            results.map { it.baseDate },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by entity ID execution time: $timeSeconds seconds")
        performanceMetrics["find_by_entity_id"] = timeSeconds
    }

    @Test
    @Order(3)
    @DisplayName("엔티티 ID와 기준일자로 데이터를 조회할 수 있다")
    fun testFindByEntityIdAndBaseDate() {
        startTime = System.currentTimeMillis()

        val entityId = allRecords.first().entityId
        val baseDate = BaseDate("202401")
        val result = skillStatsRepository.findByEntityIdAndBaseDate(entityId, baseDate)

        Assertions.assertNotNull(result)
        result?.let {
            Assertions.assertEquals(entityId, it.entityId)
            Assertions.assertEquals(baseDate.toString(), it.baseDate)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by entity ID and base date execution time: $timeSeconds seconds")
        performanceMetrics["find_by_entity_and_date"] = timeSeconds
    }

    @Test
    @Order(4)
    @DisplayName("기준일자와 엔티티 ID 목록으로 데이터를 조회할 수 있다")
    fun testFindByBaseDateAndEntityIds() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val entityIds = allRecords.take(5).map { it.entityId }
        val results = skillStatsRepository.findByBaseDateAndEntityIds(baseDate, entityIds)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all { it.baseDate == baseDate.toString() && it.entityId in entityIds },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by base date and entity IDs execution time: $timeSeconds seconds")
        performanceMetrics["find_by_date_and_entities"] = timeSeconds
    }

    @Test
    @Order(5)
    @DisplayName("기간과 엔티티 ID로 데이터를 조회할 수 있다")
    fun testFindByBaseDateBetweenAndEntityId() {
        startTime = System.currentTimeMillis()

        val entityId = allRecords.first().entityId
        val startDate = BaseDate("202401")
        val endDate = BaseDate("202403")
        val results =
            skillStatsRepository.findByBaseDateBetweenAndEntityId(
                startDate,
                endDate,
                entityId,
            )

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all {
                it.entityId == entityId &&
                    it.baseDate >= startDate.toString() &&
                    it.baseDate <= endDate.toString()
            },
        )
        Assertions.assertEquals(
            results.map { it.baseDate },
            results.map { it.baseDate }.sorted(),
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by date range and entity ID execution time: $timeSeconds seconds")
        performanceMetrics["find_by_date_range_and_entity"] = timeSeconds
    }

    @Test
    @Order(6)
    @DisplayName("엔티티 ID의 최신 통계를 조회할 수 있다")
    fun testFindLatestStatsByEntityId() {
        startTime = System.currentTimeMillis()

        val entityId = allRecords.first().entityId
        val result = skillStatsRepository.findLatestStatsByEntityId(entityId)
        log.debug("results: $result")

        Assertions.assertNotNull(result)
        result?.let {
            Assertions.assertEquals(entityId, it.entityId)
            Assertions.assertEquals("202412", it.baseDate)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find latest stats by entity ID execution time: $timeSeconds seconds")
        performanceMetrics["find_latest_stats"] = timeSeconds
    }

    @Test
    @Order(7)
    @DisplayName("성장률이 높은 상위 스킬을 찾을 수 있다")
    fun testFindTopGrowthSkills() {
        startTime = System.currentTimeMillis()

        val startDate = BaseDate("202401")
        val endDate = BaseDate("202403")
        val limit = 5
        val results = skillStatsRepository.findTopGrowthSkills(startDate, endDate, limit)

        log.debug("results: ${results.joinToString { it.toString() }}")
        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= limit)
        // Verify growth rates are in descending order
        val growthRates = results.map { it.stats.growthRate }
        Assertions.assertEquals(growthRates, growthRates.sortedDescending())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find top growth skills execution time: $timeSeconds seconds")
        performanceMetrics["find_top_growth_skills"] = timeSeconds
    }

    @Test
    @Order(8)
    @DisplayName("산업별 상위 스킬을 찾을 수 있다")
    fun testFindTopSkillsByIndustry() {
        startTime = System.currentTimeMillis()

        val industryId = industry.first().first
        val baseDate = BaseDate("202401")
        val limit = 5
        val results = skillStatsRepository.findTopSkillsByIndustry(industryId, baseDate, limit)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= limit)
        Assertions.assertTrue(
            results.all { doc ->
                doc.industryDistribution.any { it.industryId == industryId }
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find top skills by industry execution time: $timeSeconds seconds")
        performanceMetrics["find_top_skills_by_industry"] = timeSeconds
    }

    @Test
    @Order(9)
    @DisplayName("회사 규모별 상위 스킬을 찾을 수 있다")
    fun testFindTopSkillsByCompanySize() {
        startTime = System.currentTimeMillis()

        val companySize = companySize.first()
        val baseDate = BaseDate("202401")
        val limit = 5
        val results = skillStatsRepository.findTopSkillsByCompanySize(companySize, baseDate, limit)

        log.debug("results: ${results.joinToString { it.toString() }}")
        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= limit)
        Assertions.assertTrue(
            results.all { doc ->
                doc.companySizeDistribution.any { it.companySize == companySize }
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find top skills by company size execution time: $timeSeconds seconds")
        performanceMetrics["find_top_skills_by_company_size"] = timeSeconds
    }

    @Test
    @Order(11)
    @DisplayName("직무 카테고리별 상위 스킬을 찾을 수 있다")
    fun testFindTopSkillsByJobCategory() {
        startTime = System.currentTimeMillis()

        val jobCategoryId = jobCategory.first().first
        val baseDate = BaseDate("202401")
        val limit = 5
        val results = skillStatsRepository.findTopSkillsByJobCategory(jobCategoryId, baseDate, limit)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(results.size <= limit)
        Assertions.assertTrue(
            results.all { doc ->
                doc.relatedJobCategories.any { it.jobCategoryId == jobCategoryId }
            },
        )
        // Verify importance scores are in descending order
        val scores =
            results.map { doc ->
                doc.relatedJobCategories.first { it.jobCategoryId == jobCategoryId }.importanceScore
            }
        Assertions.assertEquals(scores, scores.sortedDescending())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find top skills by job category execution time: $timeSeconds seconds")
        performanceMetrics["find_top_skills_by_job_category"] = timeSeconds
    }

    @Test
    @Order(12)
    @DisplayName("여러 산업에서 성장하는 스킬을 찾을 수 있다")
    fun testFindSkillsWithMultiIndustryGrowth() {
        startTime = System.currentTimeMillis()

        val baseDate = BaseDate("202401")
        val minIndustryCount = 3
        val minGrowthRate = 20.0
        val results = skillStatsRepository.findSkillsWithMultiIndustryGrowth(baseDate, minIndustryCount, minGrowthRate)

        Assertions.assertTrue(
            results.all { doc ->
                doc.industryDistribution.size >= minIndustryCount &&
                    doc.stats.growthRate >= minGrowthRate
            },
        )

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find multi-industry growth skills execution time: $timeSeconds seconds")
        performanceMetrics["find_multi_industry_growth"] = timeSeconds
    }

    @Test
    @Order(13)
    @DisplayName("산업별 신흥 스킬을 찾을 수 있다")
    fun testFindEmergingSkillsByIndustry() {
        startTime = System.currentTimeMillis()

        val industryId = industry.first().first
        val baseDate = BaseDate("202401")
        val minGrowthRate = 30.0
        val results = skillStatsRepository.findEmergingSkillsByIndustry(industryId, baseDate, minGrowthRate)

        Assertions.assertTrue(results.isNotEmpty())
        Assertions.assertTrue(
            results.all { doc ->
                doc.isEmergingSkill &&
                    doc.stats.growthRate >= minGrowthRate &&
                    doc.industryDistribution.any { it.industryId == industryId }
            },
        )
        // Verify growth rates are in descending order
        val growthRates = results.map { it.stats.growthRate }
        Assertions.assertEquals(growthRates, growthRates.sortedDescending())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find emerging skills by industry execution time: $timeSeconds seconds")
        performanceMetrics["find_emerging_skills"] = timeSeconds
    }

    @Test
    @Order(14)
    @DisplayName("대량의 데이터를 삭제할 수 있다")
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        var totalDeleted = 0

        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = skillStatsRepository.bulkDelete(batch)
            totalDeleted += deletedCount
        }

        val remainingRecords = skillStatsRepository.findAllByQuery(Query())
        Assertions.assertTrue(remainingRecords.isEmpty())

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }
}
