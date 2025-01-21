package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "industry_growth_rankings")
class IndustryGrowthRankingsDocument(
    id: String? = null,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: IndustryGrowthMetrics,
    @Field("rankings")
    override val rankings: List<IndustryGrowthRankingEntry>,
) : SimpleRankingDocument<IndustryGrowthRankingsDocument.IndustryGrowthRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
    ) {
    data class IndustryGrowthMetrics(
        @Field("total_count")
        override val totalCount: Int,
        @Field("ranked_count")
        override val rankedCount: Int,
        @Field("new_entries")
        override val newEntries: Int,
        @Field("dropped_entries")
        override val droppedEntries: Int,
        @Field("volatility_metrics")
        override val volatilityMetrics: VolatilityMetrics,
        @Field("economic_indicators")
        val economicIndicators: EconomicIndicators,
    ) : RankingMetrics {
        data class EconomicIndicators(
            @Field("gdp_correlation")
            val gdpCorrelation: Double,
            @Field("employment_impact")
            val employmentImpact: Double,
            @Field("investment_trends")
            val investmentTrends: InvestmentTrends,
        ) {
            data class InvestmentTrends(
                val totalInvestment: Long,
                val growthRate: Double,
                val investorConfidence: Double,
            )
        }
    }

    data class IndustryGrowthRankingEntry(
        @Field("document_id")
        override val documentId: String,
        @Field("entity_id")
        override val entityId: Long,
        @Field("name")
        override val name: String,
        @Field("rank")
        override val rank: Int,
        @Field("previous_rank")
        override val previousRank: Int?,
        @Field("rank_change")
        override val rankChange: Int?,
        @Field("value")
        override val score: Double,
        @Field("growth_analysis")
        val growthAnalysis: GrowthAnalysis,
        @Field("market_maturity")
        val marketMaturity: MarketMaturity,
    ) : SimpleRankingEntry {
        data class GrowthAnalysis(
            @Field("revenue_growth")
            val revenueGrowth: Double,
            @Field("employment_growth")
            val employmentGrowth: Double,
            @Field("market_share_growth")
            val marketShareGrowth: Double,
            @Field("innovation_index")
            val innovationIndex: Double,
        )

        data class MarketMaturity(
            @Field("stage")
            val stage: String,
            @Field("saturation_level")
            val saturationLevel: Double,
            @Field("competition_intensity")
            val competitionIntensity: Double,
            @Field("barrier_to_entry")
            val barrierToEntry: Double,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(
            rankings.all {
                it.growthAnalysis.revenueGrowth >= -100.0
            },
        ) { "Revenue growth cannot be less than -100%" }
    }
}
