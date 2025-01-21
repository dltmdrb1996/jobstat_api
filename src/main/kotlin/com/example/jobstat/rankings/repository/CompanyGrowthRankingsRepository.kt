package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.rankings.model.CompanyGrowthRankingsDocument
import com.mongodb.client.model.*
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.aggregation.Aggregation.ROOT
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface CompanyGrowthRankingsRepository : SimpleRankingRepository<CompanyGrowthRankingsDocument, CompanyGrowthRankingsDocument.CompanyGrowthRankingEntry, String> {
    // 다각적 성장 분석 (매출, 직원, 시장점유율 모두 성장하는 기업)
    fun findBalancedGrowthCompanies(
        baseDate: String,
        minGrowthRate: Double,
    ): List<CompanyGrowthRankingsDocument>

    // 산업별 성장 리더 분석
    fun findIndustryGrowthLeaders(
        baseDate: String,
        industryId: Long,
    ): List<CompanyGrowthRankingsDocument>

    // 지속 가능한 성장 분석
    fun findSustainableGrowthCompanies(
        months: Int,
        minProfitability: Double,
        minGrowthRate: Double,
    ): List<CompanyGrowthRankingsDocument>
}

@Repository
class CompanyGrowthRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanyGrowthRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<CompanyGrowthRankingsDocument, CompanyGrowthRankingsDocument.CompanyGrowthRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanyGrowthRankingsRepository {
    override fun findBalancedGrowthCompanies(
        baseDate: String,
        minGrowthRate: Double,
    ): List<CompanyGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("rankings.growth_metrics.revenue_growth", minGrowthRate),
                        Filters.gte("rankings.growth_metrics.employee_growth", minGrowthRate),
                        Filters.gte("rankings.growth_metrics.market_share_growth", minGrowthRate),
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("rankings.growth_metrics.revenue_growth"),
                        Sorts.descending("rankings.growth_metrics.employee_growth"),
                        Sorts.descending("rankings.growth_metrics.market_share_growth"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findIndustryGrowthLeaders(
        baseDate: String,
        industryId: Long,
    ): List<CompanyGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "industry_growth_metrics",
                        Document(
                            "\$getField",
                            Document("field", industryId.toString())
                                .append("input", "\$metrics.market_metrics.industry_context"),
                        ),
                    ),
                ),
                Aggregates.match(
                    Filters.gt("industry_growth_metrics.industryGrowth", 0),
                ),
                Aggregates.sort(Sorts.descending("rankings.growth_metrics.revenue_growth")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSustainableGrowthCompanies(
        months: Int,
        minProfitability: Double,
        minGrowthRate: Double,
    ): List<CompanyGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.sort(Sorts.descending("base_date")),
                Aggregates.limit(months),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("rankings.business_metrics.financial_health.profitability", minProfitability),
                        Filters.gte("rankings.growth_metrics.revenue_growth", minGrowthRate),
                    ),
                ),
                Aggregates.group(
                    "\$rankings.entity_id",
                    Accumulators.avg("avg_profitability", "\$rankings.business_metrics.financial_health.profitability"),
                    Accumulators.avg("avg_growth", "\$rankings.growth_metrics.revenue_growth"),
                    Accumulators.first("latest", "$$ROOT"),
                ),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("avg_profitability", minProfitability),
                        Filters.gte("avg_growth", minGrowthRate),
                    ),
                ),
                Aggregates.sort(Sorts.descending("avg_growth")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc ->
                doc.get("latest", Document::class.java)?.let {
                    mongoOperations.converter.read(entityInformation.javaType, it)
                } ?: throw IllegalStateException("Document does not contain 'latest' field")
            }.filterNotNull()
            .toList()
    }
}
