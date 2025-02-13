package com.example.jobstat.statistics.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics.rankings.document.JobCategorySalaryRankingsDocument
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.JOB_CATEGORY_SALARY)
@NoRepositoryBean
interface JobCategorySalaryRankingsRepository : SimpleRankingRepository<JobCategorySalaryRankingsDocument, JobCategorySalaryRankingsDocument.JobCategorySalaryRankingEntry, String> {
    // 생활비 조정 급여 분석
    fun findSalaryByIndustry(
        baseDate: String,
        industryId: Long,
        minMarketPosition: Double,
    ): List<JobCategorySalaryRankingsDocument>

    // 원격 근무 영향도 분석
    fun findByExperienceImpact(
        baseDate: String,
        experienceLevel: String,
        minPremium: Double,
    ): List<JobCategorySalaryRankingsDocument>

    // 지역별 산업 집중도 분석
    fun findBySizeVariance(
        baseDate: String,
        companySize: String,
        maxVariance: Double,
    ): List<JobCategorySalaryRankingsDocument>
}

@Repository
class JobCategorySalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<JobCategorySalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<JobCategorySalaryRankingsDocument, JobCategorySalaryRankingsDocument.JobCategorySalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    JobCategorySalaryRankingsRepository {
    override fun findSalaryByIndustry(
        baseDate: String,
        industryId: Long,
        minMarketPosition: Double,
    ): List<JobCategorySalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$gte",
                            listOf(
                                Document(
                                    "\$getField",
                                    listOf(
                                        "market_position",
                                        Document(
                                            "\$getField",
                                            listOf(
                                                industryId.toString(),
                                                "\$metrics.salary_analysis.industry_comparison",
                                            ),
                                        ),
                                    ),
                                ),
                                minMarketPosition,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.salary_analysis.industry_comparison.$industryId.market_position",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByExperienceImpact(
        baseDate: String,
        experienceLevel: String,
        minPremium: Double,
    ): List<JobCategorySalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$gte",
                            listOf(
                                Document(
                                    "\$getField",
                                    listOf(
                                        experienceLevel,
                                        "\$metrics.salary_analysis.experience_impact",
                                    ),
                                ),
                                minPremium,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.salary_analysis.experience_impact.$experienceLevel",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findBySizeVariance(
        baseDate: String,
        companySize: String,
        maxVariance: Double,
    ): List<JobCategorySalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$lte",
                            listOf(
                                Document(
                                    "\$getField",
                                    listOf(
                                        companySize,
                                        "\$metrics.salary_analysis.company_size_variance",
                                    ),
                                ),
                                maxVariance,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.ascending(
                        "metrics.salary_analysis.company_size_variance.$companySize",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
