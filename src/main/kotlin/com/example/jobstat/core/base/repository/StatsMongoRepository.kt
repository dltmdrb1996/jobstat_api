package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface StatsMongoRepository<T : BaseStatsDocument, ID : Any> : BaseTimeSeriesRepository<T, ID> {
    fun findByEntityId(entityId: Long): List<T>

    fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T?

    fun findByBaseDateAndEntityIds(
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T>

    fun findByBaseDateBetweenAndEntityId(
        startDate: BaseDate,
        endDate: BaseDate,
        entityId: Long,
    ): List<T>

    fun findLatestStatsByEntityId(entityId: Long): T?

    fun findTopGrowthSkills(
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<T>

    // 특정 산업에서 수요가 높은 스킬들 조회
    fun findTopSkillsByIndustry(
        industryId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    // 특정 기업 규모별 선호 스킬 조회
    fun findTopSkillsByCompanySize(
        companySize: String,
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    // 연관 직무 카테고리에서 중요도가 높은 스킬들 조회
    fun findTopSkillsByJobCategory(
        jobCategoryId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    // 경력 레벨별 평균 급여가 높은 스킬들 조회
    fun findTopSalarySkillsByExperienceLevel(
        experienceRange: String,
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    fun findEmergingSkillsByIndustry(
        industryId: Long,
        baseDate: BaseDate,
        minGrowthRate: Double,
    ): List<T>

    fun findSkillsWithMultiIndustryGrowth(
        baseDate: BaseDate,
        minIndustryCount: Int,
        minGrowthRate: Double,
    ): List<T>
}

abstract class StatsMongoRepositoryImpl<T : BaseStatsDocument, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseTimeSeriesRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    StatsMongoRepository<T, ID> {
    /**
     * 특정 엔티티의 모든 통계 데이터를 조회합니다.
     * 기준일자를 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param entityId 조회할 엔티티 ID
     * @return 해당 엔티티의 모든 통계 데이터 목록
     */
    override fun findByEntityId(entityId: Long): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("entity_id", entityId))
            .sort(Sorts.descending("base_date"))
            .hintString("snapshot_lookup_idx")
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티의 특정 기준일자 통계 데이터를 조회합니다.
     *
     * @param entityId 엔티티 ID
     * @param baseDate 기준 날짜
     * @return 해당 엔티티의 통계 데이터, 없으면 null
     */
    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("entity_id", entityId),
                    Filters.eq("base_date", baseDate.toString()),
                ),
            ).hintString("snapshot_lookup_idx")
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 특정 기준일자의 여러 엔티티 통계 데이터를 조회합니다.
     * entityIds가 비어있는 경우 빈 리스트를 반환합니다.
     *
     * @param baseDate 기준 날짜
     * @param entityIds 조회할 엔티티 ID 목록
     * @return 해당 엔티티들의 통계 데이터 목록
     */
    override fun findByBaseDateAndEntityIds(
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T> {
        if (entityIds.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.`in`("entity_id", entityIds),
                ),
            ).hintString("snapshot_lookup_idx")
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 기간 동안의 엔티티 통계 데이터를 조회합니다.
     * 기준일자를 기준으로 오름차순 정렬하여 반환합니다.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param entityId 조회할 엔티티 ID
     * @return 해당 기간의 통계 데이터 목록
     */
    override fun findByBaseDateBetweenAndEntityId(
        startDate: BaseDate,
        endDate: BaseDate,
        entityId: Long,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("entity_id", entityId),
                    Filters.gte("base_date", startDate.toString()),
                    Filters.lte("base_date", endDate.toString()),
                ),
            ).sort(Sorts.ascending("base_date"))
            .hintString("snapshot_lookup_idx")
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티의 가장 최근 통계 데이터를 조회합니다.
     *
     * @param entityId 조회할 엔티티 ID
     * @return 가장 최근 통계 데이터, 없으면 null
     */
    override fun findLatestStatsByEntityId(entityId: Long): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("entity_id", entityId))
            .sort(Sorts.descending("base_date"))
            .limit(1)
            .hintString("snapshot_lookup_idx")
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 특정 기간 동안 가장 높은 성장률을 보인 스킬들을 조회합니다.
     * 성장률을 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param limit 조회할 스킬 수
     * @return 성장률이 높은 스킬 목록
     */
    override fun findTopGrowthSkills(
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.gte("base_date", startDate.toString()),
                        Filters.lte("base_date", endDate.toString()),
                    ),
                ),
                Aggregates.sort(Sorts.descending("stats.growth_rate")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 산업에서 수요가 높은 스킬들을 조회합니다.
     * 산업 내 빈도수를 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param industryId 산업 ID
     * @param baseDate 기준 날짜
     * @param limit 조회할 스킬 수
     * @return 산업별 상위 스킬 목록
     */
    override fun findTopSkillsByIndustry(
        industryId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                        Filters.elemMatch(
                            "industry_distribution",
                            Filters.eq("industry_id", industryId),
                        ),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "industryStats",
                        Document(
                            "\$filter",
                            Document(
                                mapOf(
                                    "input" to "\$industry_distribution",
                                    "as" to "ind",
                                    "cond" to Document("\$eq", listOf("\$\$ind.industry_id", industryId)),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("industryStats.count")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 경력 레벨에서 평균 급여가 높은 스킬들을 조회합니다.
     * 평균 급여를 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param experienceRange 경력 범위
     * @param baseDate 기준 날짜
     * @param limit 조회할 스킬 수
     * @return 급여가 높은 스킬 목록
     */
    override fun findTopSalarySkillsByExperienceLevel(
        experienceRange: String,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                Aggregates.addFields(
                    Field(
                        "filtered_experience_levels",
                        Document(
                            "\$filter",
                            Document(
                                mapOf(
                                    "input" to "\$experience_levels",
                                    "as" to "exp",
                                    "cond" to Document("\$eq", listOf("\$\$exp.range", experienceRange)),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.match(Filters.gt("filtered_experience_levels.0", Document())),
                Aggregates.addFields(
                    Field(
                        "max_avg_salary",
                        Document("\$max", "\$filtered_experience_levels.avg_salary"),
                    ),
                ),
                Aggregates.sort(Sorts.descending("max_avg_salary")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 기업 규모에서 선호되는 스킬들을 조회합니다.
     * 기업 규모별 빈도수를 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param companySize 기업 규모
     * @param baseDate 기준 날짜
     * @param limit 조회할 스킬 수
     * @return 기업 규모별 상위 스킬 목록
     */
    override fun findTopSkillsByCompanySize(
        companySize: String,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                Aggregates.addFields(
                    Field(
                        "company_size_distribution",
                        Document(
                            "\$filter",
                            Document(
                                mapOf(
                                    "input" to "\$company_size_distribution",
                                    "as" to "c",
                                    "cond" to Document("\$eq", listOf("\$\$c.company_size", companySize)),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.match(Filters.exists("company_size_distribution.0")),
                Aggregates.sort(Sorts.descending("company_size_distribution.count")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 직무 카테고리에서 중요도가 높은 스킬들을 조회합니다.
     * 중요도 점수를 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param jobCategoryId 직무 카테고리 ID
     * @param baseDate 기준 날짜
     * @param limit 조회할 스킬 수
     * @return 직무 카테고리별 상위 스킬 목록
     */
    override fun findTopSkillsByJobCategory(
        jobCategoryId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                        Filters.elemMatch(
                            "related_job_categories",
                            Filters.eq("job_category_id", jobCategoryId),
                        ),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "job_category_stats",
                        Document(
                            "\$filter",
                            Document(
                                mapOf(
                                    "input" to "\$related_job_categories",
                                    "as" to "job",
                                    "cond" to Document("\$eq", listOf("\$\$job.job_category_id", jobCategoryId)),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("job_category_stats.importance_score")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 여러 산업에서 동시에 성장하고 있는 스킬들을 조회합니다.
     * 지정된 수 이상의 산업에서 최소 성장률 이상의 성장을 보이는 스킬들을 반환합니다.
     * 성장률을 기준으로 내림차순 정렬됩니다.
     *
     * @param baseDate 기준 날짜
     * @param minIndustryCount 최소 산업 수
     * @param minGrowthRate 최소 성장률
     * @return 다중 산업 성장 스킬 목록
     */
    override fun findSkillsWithMultiIndustryGrowth(
        baseDate: BaseDate,
        minIndustryCount: Int,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("stats.growth_rate", minGrowthRate),
                        Document(
                            "\$expr",
                            Document(
                                "\$gte",
                                listOf(
                                    Document("\$size", "\$industry_distribution"),
                                    minIndustryCount,
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("stats.growth_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 산업에서 새롭게 부상하는 스킬들을 조회합니다.
     * emerging_skill이 true이고 지정된 최소 성장률 이상을 보이는 스킬들을 반환합니다.
     * 성장률을 기준으로 내림차순 정렬됩니다.
     *
     * @param industryId 산업 ID
     * @param baseDate 기준 날짜
     * @param minGrowthRate 최소 성장률
     * @return 새롭게 부상하는 스킬 목록
     */
    override fun findEmergingSkillsByIndustry(
        industryId: Long,
        baseDate: BaseDate,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                        Filters.eq("emerging_skill", true),
                        Filters.gte("stats.growth_rate", minGrowthRate),
                        Filters.elemMatch(
                            "industry_distribution",
                            Filters.eq("industry_id", industryId),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("stats.growth_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
