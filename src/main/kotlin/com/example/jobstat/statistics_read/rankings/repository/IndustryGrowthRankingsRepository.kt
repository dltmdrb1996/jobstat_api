package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.core.core_mongo_base.repository.SimpleRankingRepository
import com.example.jobstat.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics_read.rankings.document.IndustryGrowthRankingsDocument
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.INDUSTRY_GROWTH)
@NoRepositoryBean
interface IndustryGrowthRankingsRepository :
    SimpleRankingRepository<IndustryGrowthRankingsDocument, IndustryGrowthRankingsDocument.IndustryGrowthRankingEntry, String> {
    // 혁신 지수 기반 분석
    fun findHighInnovationIndustries(
        baseDate: String,
        minInnovationIndex: Double,
    ): List<IndustryGrowthRankingsDocument>

    // 경제 지표 연관성 분석
    fun findEconomicIndicatorCorrelation(
        baseDate: String,
        minGdpCorrelation: Double,
    ): List<IndustryGrowthRankingsDocument>

    // 시장 성숙도 기반 분석
    fun findByMarketMaturityStage(
        baseDate: String,
        stage: String,
    ): List<IndustryGrowthRankingsDocument>
}

@Repository
class IndustryGrowthRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<IndustryGrowthRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<IndustryGrowthRankingsDocument, IndustryGrowthRankingsDocument.IndustryGrowthRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    IndustryGrowthRankingsRepository {
    override fun findHighInnovationIndustries(
        baseDate: String,
        minInnovationIndex: Double,
    ): List<IndustryGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("growth_analysis.innovation_index", minInnovationIndex),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("growth_analysis.innovation_index"),
                        Sorts.descending("growth_analysis.revenue_growth"),
                    ),
                ),
            )
        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEconomicIndicatorCorrelation(
        baseDate: String,
        minGdpCorrelation: Double,
    ): List<IndustryGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("metrics.economic_indicators.gdp_correlation", minGdpCorrelation),
                ),
                Aggregates.addFields(
                    Field(
                        "economic_impact_score",
                        Document(
                            "\$add",
                            listOf(
                                "\$metrics.economic_indicators.gdp_correlation",
                                "\$metrics.economic_indicators.employment_impact",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("economic_impact_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMarketMaturityStage(
        baseDate: String,
        stage: String,
    ): List<IndustryGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(Filters.eq("market_maturity.stage", stage)),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.ascending("market_maturity.saturation_level"),
                        Sorts.descending("growth_analysis.revenue_growth"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
