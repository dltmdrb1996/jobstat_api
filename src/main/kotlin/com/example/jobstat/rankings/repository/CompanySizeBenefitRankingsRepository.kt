package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.repository.DistributionRankingRepository
import com.example.jobstat.core.base.repository.DistributionRankingRepositoryImpl
import com.example.jobstat.core.state.CompanySize
import com.example.jobstat.rankings.model.CompanySizeBenefitRankingsDocument
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface CompanySizeBenefitRankingsRepository
    :
    DistributionRankingRepository<CompanySizeBenefitRankingsDocument, CompanySizeBenefitRankingsDocument.CompanySizeBenefitRankingEntry, String> {
    // 회사 규모별 특화 복리후생 패턴 분석
    fun findDistinctiveBenefitsByCompanySize(
        baseDate: String,
        companySize: CompanySize,
    ): List<CompanySizeBenefitRankingsDocument>

    // 복리후생 만족도 기반 분석
    fun findHighestSatisfactionBenefits(
        baseDate: String,
        minSatisfactionScore: Double,
    ): List<CompanySizeBenefitRankingsDocument>

    // 비용 효율적인 복리후생 분석
    fun findCostEffectiveBenefits(
        baseDate: String,
        maxMonetaryValue: Long,
        minSatisfactionScore: Double,
    ): List<CompanySizeBenefitRankingsDocument>
}

@Repository
class CompanySizeBenefitRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanySizeBenefitRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : DistributionRankingRepositoryImpl<CompanySizeBenefitRankingsDocument, CompanySizeBenefitRankingsDocument.CompanySizeBenefitRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanySizeBenefitRankingsRepository {
    override fun findDistinctiveBenefitsByCompanySize(
        baseDate: String,
        companySize: CompanySize,
    ): List<CompanySizeBenefitRankingsDocument> {
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
                                        companySize.name,
                                        "\$distribution",
                                    ),
                                ),
                                Document(
                                    "\$multiply",
                                    listOf(
                                        Document("\$avg", "\$distribution"),
                                        1.5, // 평균보다 50% 이상 높은 경우를 distinctive로 판단
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("benefit_metrics.satisfaction_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHighestSatisfactionBenefits(
        baseDate: String,
        minSatisfactionScore: Double,
    ): List<CompanySizeBenefitRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("benefit_metrics.satisfaction_score", minSatisfactionScore),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("benefit_metrics.satisfaction_score"),
                        Sorts.descending("benefit_metrics.provision_rate"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findCostEffectiveBenefits(
        baseDate: String,
        maxMonetaryValue: Long,
        minSatisfactionScore: Double,
    ): List<CompanySizeBenefitRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.and(
                        Filters.lte("benefit_metrics.monetary_value", maxMonetaryValue),
                        Filters.gte("benefit_metrics.satisfaction_score", minSatisfactionScore),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "cost_effectiveness",
                        Document(
                            "\$divide",
                            listOf(
                                "\$benefit_metrics.satisfaction_score",
                                "\$benefit_metrics.monetary_value",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("cost_effectiveness")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
