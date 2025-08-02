package com.wildrew.jobstat.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.wildrew.jobstat.statistics_read.rankings.document.BenefitPostingCountRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.document.BenefitPostingCountRankingsDocument.BenefitPostingRankingEntry
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface BenefitPostingCountRankingsRepository :
    SimpleRankingRepository<BenefitPostingCountRankingsDocument, BenefitPostingRankingEntry, String> {
    // 산업별 특화 복리후생 분석
    fun findUniqueIndustryBenefits(
        baseDate: String,
        industryId: Long,
    ): List<BenefitPostingCountRankingsDocument>

    // 임팩트 기반 복리후생 분석
    fun findHighImpactBenefits(
        baseDate: String,
        minRetentionImpact: Double,
    ): List<BenefitPostingCountRankingsDocument>

    // 기업 규모별 복리후생 트렌드 분석
    fun findBenefitTrendsByCompanySize(
        baseDate: String,
        companySize: String,
    ): List<BenefitPostingCountRankingsDocument>
}

@Repository
@RankingRepositoryType(RankingType.BENEFIT_POSTING_COUNT)
class BenefitPostingCountRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<BenefitPostingCountRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<BenefitPostingCountRankingsDocument, BenefitPostingRankingEntry, String>(
    entityInformation,
    mongoOperations,
),
    BenefitPostingCountRankingsRepository {
    override fun findUniqueIndustryBenefits(
        baseDate: String,
        industryId: Long,
    ): List<BenefitPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "industry_benefits",
                        Document(
                            "\$getField",
                            Document("field", industryId.toString())
                                .append("input", "\$metrics.benefit_metrics.industry_analysis"),
                        ),
                    ),
                ),
                Aggregates.match(
                    Filters.exists("industry_benefits.unique_benefits", true),
                ),
                Aggregates.sort(Sorts.descending("industry_benefits.offering_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHighImpactBenefits(
        baseDate: String,
        minRetentionImpact: Double,
    ): List<BenefitPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte(
                        "rankings.offering_metrics.employee_impact.retention_impact",
                        minRetentionImpact,
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("rankings.offering_metrics.employee_impact.retention_impact"),
                        Sorts.descending("rankings.offering_metrics.employee_impact.satisfaction_impact"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findBenefitTrendsByCompanySize(
        baseDate: String,
        companySize: String,
    ): List<BenefitPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$gt",
                            listOf(
                                Document(
                                    "\$getField",
                                    listOf(
                                        companySize,
                                        "\$rankings.offering_metrics.company_distribution.by_size",
                                    ),
                                ),
                                0.0,
                            ),
                        ),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "size_offering_rate",
                        Document(
                            "\$getField",
                            listOf(
                                companySize,
                                "\$rankings.offering_metrics.company_distribution.by_size",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("size_offering_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
