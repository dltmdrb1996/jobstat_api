package com.wildrew.jobstat.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.wildrew.jobstat.statistics_read.rankings.document.SkillSalaryRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface SkillSalaryRankingsRepository : SimpleRankingRepository<SkillSalaryRankingsDocument, SkillSalaryRankingsDocument.SkillSalaryRankingEntry, String> {
    // 산업별 급여 프리미엄 분석
    fun findByIndustryPremium(
        baseDate: String,
        industryId: Long,
        minPremium: Double,
    ): List<SkillSalaryRankingsDocument>

    // 경력별 급여 상승률 분석
    fun findByExperiencePremium(
        baseDate: String,
        experienceLevel: String,
        minPremium: Double,
    ): List<SkillSalaryRankingsDocument>

    // 급여 분포 분석
    fun findBySalaryDistribution(
        baseDate: String,
        percentile: Int,
        minSalary: Long,
    ): List<SkillSalaryRankingsDocument>
}

@Repository
@RankingRepositoryType(RankingType.SKILL_SALARY)
class SkillSalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<SkillSalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<SkillSalaryRankingsDocument, SkillSalaryRankingsDocument.SkillSalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    SkillSalaryRankingsRepository {
    override fun findByIndustryPremium(
        baseDate: String,
        industryId: Long,
        minPremium: Double,
    ): List<SkillSalaryRankingsDocument> {
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
                                        industryId.toString(),
                                        "\$metrics.salary_distribution.industry_comparison",
                                    ),
                                ),
                                minPremium,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.salary_distribution.industry_comparison.$industryId",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByExperiencePremium(
        baseDate: String,
        experienceLevel: String,
        minPremium: Double,
    ): List<SkillSalaryRankingsDocument> {
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
                                        "\$experience_premium",
                                    ),
                                ),
                                minPremium,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("experience_premium.$experienceLevel")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findBySalaryDistribution(
        baseDate: String,
        percentile: Int,
        minSalary: Long,
    ): List<SkillSalaryRankingsDocument> {
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
                                        percentile.toString(),
                                        "\$metrics.salary_distribution.percentiles",
                                    ),
                                ),
                                minSalary,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("metrics.salary_distribution.percentiles.$percentile"),
                        Sorts.descending("metrics.salary_distribution.median_salary"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
