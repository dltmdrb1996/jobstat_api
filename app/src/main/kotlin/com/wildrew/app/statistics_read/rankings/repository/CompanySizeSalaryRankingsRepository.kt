package com.wildrew.app.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.app.statistics_read.core.core_model.ExperienceLevel
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.DistributionRankingRepository
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.DistributionRankingRepositoryImpl
import com.wildrew.app.statistics_read.rankings.document.CompanySizeSalaryRankingsDocument
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.COMPANY_SIZE_SALARY)
@NoRepositoryBean
interface CompanySizeSalaryRankingsRepository :
    DistributionRankingRepository<CompanySizeSalaryRankingsDocument, CompanySizeSalaryRankingsDocument.CompanySizeSalaryRankingEntry, String> {
    // 급여 공정성 분석
    fun findSalaryEquityByCompanySize(
        baseDate: String,
        maxVarianceRatio: Double,
    ): List<CompanySizeSalaryRankingsDocument>

    // 경험 수준별 급여 분포 분석
    fun findSalaryDistributionByExperience(
        baseDate: String,
        experienceLevel: ExperienceLevel,
    ): List<CompanySizeSalaryRankingsDocument>

    // 보상 패키지 구성 분석
    fun findCompensationPackageDistribution(
        baseDate: String,
        minBaseSalaryRatio: Double,
    ): List<CompanySizeSalaryRankingsDocument>
}

@Repository
class CompanySizeSalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanySizeSalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : DistributionRankingRepositoryImpl<CompanySizeSalaryRankingsDocument, CompanySizeSalaryRankingsDocument.CompanySizeSalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanySizeSalaryRankingsRepository {
    override fun findSalaryEquityByCompanySize(
        baseDate: String,
        maxVarianceRatio: Double,
    ): List<CompanySizeSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "variance_ratio",
                        Document(
                            "\$divide",
                            listOf(
                                Document(
                                    "\$subtract",
                                    listOf(
                                        "\$salary_metrics.percentile_range.p75",
                                        "\$salary_metrics.percentile_range.p25",
                                    ),
                                ),
                                "\$salary_metrics.median_salary",
                            ),
                        ),
                    ),
                ),
                Aggregates.match(Filters.lte("variance_ratio", maxVarianceRatio)),
                Aggregates.sort(Sorts.ascending("variance_ratio")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSalaryDistributionByExperience(
        baseDate: String,
        experienceLevel: ExperienceLevel,
    ): List<CompanySizeSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.exists("experience_distribution.${experienceLevel.name}", true),
                ),
                Aggregates.sort(
                    Sorts.descending("experience_distribution.${experienceLevel.name}.median"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findCompensationPackageDistribution(
        baseDate: String,
        minBaseSalaryRatio: Double,
    ): List<CompanySizeSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte(
                        "salary_metrics.compensation_ratio.base_salary",
                        minBaseSalaryRatio,
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("salary_metrics.compensation_ratio.base_salary"),
                        Sorts.descending("salary_metrics.median_salary"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
