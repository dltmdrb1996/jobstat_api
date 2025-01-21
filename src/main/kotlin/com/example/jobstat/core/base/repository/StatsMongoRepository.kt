package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
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
        baseDate: String,
    ): T?

    fun findByBaseDateAndEntityIds(
        baseDate: String,
        entityIds: List<Long>,
    ): List<T>

    fun findByBaseDateBetweenAndEntityId(
        startDate: String,
        endDate: String,
        entityId: Long,
    ): List<T>

    fun findLatestStatsByEntityId(entityId: Long): T?

    fun findTopGrowthSkills(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T>

    // 특정 산업에서 수요가 높은 스킬들 조회
    fun findTopSkillsByIndustry(
        industryId: Long,
        baseDate: String,
        limit: Int,
    ): List<T>

    // 특정 기업 규모별 선호 스킬 조회
    fun findTopSkillsByCompanySize(
        companySize: String,
        baseDate: String,
        limit: Int,
    ): List<T>

    // 연관 직무 카테고리에서 중요도가 높은 스킬들 조회
    fun findTopSkillsByJobCategory(
        jobCategoryId: Long,
        baseDate: String,
        limit: Int,
    ): List<T>

    // 경력 레벨별 평균 급여가 높은 스킬들 조회
    fun findTopSalarySkillsByExperienceLevel(
        experienceRange: String,
        baseDate: String,
        limit: Int,
    ): List<T>

    fun findEmergingSkillsByIndustry(
        industryId: Long,
        baseDate: String,
        minGrowthRate: Double,
    ): List<T>

    fun findSkillsWithMultiIndustryGrowth(
        baseDate: String,
        minIndustryCount: Int,
        minGrowthRate: Double,
    ): List<T>
}

abstract class StatsMongoRepositoryImpl<T : BaseStatsDocument, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseTimeSeriesRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    StatsMongoRepository<T, ID> {
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

    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("entity_id", entityId),
                    Filters.eq("base_date", baseDate),
                ),
            ).hintString("snapshot_lookup_idx")
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByBaseDateAndEntityIds(
        baseDate: String,
        entityIds: List<Long>,
    ): List<T> {
        if (entityIds.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.`in`("entity_id", entityIds),
                ),
            ).hintString("snapshot_lookup_idx")
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByBaseDateBetweenAndEntityId(
        startDate: String,
        endDate: String,
        entityId: Long,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("entity_id", entityId),
                    Filters.gte("base_date", startDate),
                    Filters.lte("base_date", endDate),
                ),
            ).sort(Sorts.ascending("base_date"))
            .hintString("snapshot_lookup_idx")
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

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

    override fun findTopGrowthSkills(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.gte("base_date", startDate),
                        Filters.lte("base_date", endDate),
                    ),
                ),
                Aggregates.sort(Sorts.descending("stats.growth_rate")),
                Aggregates.limit(limit),
                // 프로젝션 제거
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopSkillsByIndustry(
        industryId: Long,
        baseDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate),
                        Filters.elemMatch(
                            "industry_distribution", // @Field 애노테이션에 맞춰 스네이크 케이스 사용
                            Filters.eq("industry_id", industryId), // @Field 애노테이션에 맞춰 스네이크 케이스 사용
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
                                    "input" to "\$industry_distribution", // 스네이크 케이스 사용
                                    "as" to "ind",
                                    "cond" to Document("\$eq", listOf("\$\$ind.industry_id", industryId)), // 스네이크 케이스 사용
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

    override fun findTopSalarySkillsByExperienceLevel(
        experienceRange: String,
        baseDate: String,
        limit: Int,
    ): List<T> { // Replace `SkillStatsDocument` with your actual type if different
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                // Stage 1: Match documents with the specified base_date
                Aggregates.match(Filters.eq("base_date", baseDate)),
                // Stage 2: Add a new field 'filtered_experience_levels' by filtering the 'experience_levels' array
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
                // Stage 3: Ensure that the 'filtered_experience_levels' array is not empty
                Aggregates.match(Filters.gt("filtered_experience_levels.0", Document())),
                // Stage 4: Add a new field 'max_avg_salary' by extracting the maximum 'avg_salary' from the filtered array
                Aggregates.addFields(
                    Field(
                        "max_avg_salary",
                        Document("\$max", "\$filtered_experience_levels.avg_salary"),
                    ),
                ),
                // Stage 5: Sort the documents in descending order based on 'max_avg_salary'
                Aggregates.sort(Sorts.descending("max_avg_salary")),
                // Stage 6: Limit the number of returned documents
                Aggregates.limit(limit),
                // Optional Stage 7: Project the necessary fields (remove if you want the entire document)
                // Aggregates.project(
                //     Projections.exclude("max_avg_salary", "filtered_experience_levels")
                // )
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopSkillsByCompanySize(
        companySize: String,
        baseDate: String,
        limit: Int,
    ): List<T> { // Replace `List<T>` with the actual type if necessary
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
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

    override fun findTopSkillsByJobCategory(
        jobCategoryId: Long,
        baseDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate),
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

    // 추가 유용한 메서드들:
    override fun findSkillsWithMultiIndustryGrowth(
        baseDate: String,
        minIndustryCount: Int,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
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

    override fun findEmergingSkillsByIndustry(
        industryId: Long,
        baseDate: String,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate),
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
