package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "job_category_growth_rankings")
class JobCategoryGrowthRankingsDocument(
    id: String? = null,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: JobCategoryGrowthMetrics,
    @Field("rankings")
    override val rankings: List<JobCategoryGrowthRankingEntry>,
) : SimpleRankingDocument<JobCategoryGrowthRankingsDocument.JobCategoryGrowthRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
    ) {
    data class JobCategoryGrowthMetrics(
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
        @Field("market_dynamics")
        val marketDynamics: MarketDynamics,
    ) : RankingMetrics {
        data class MarketDynamics(
            @Field("industry_growth_correlation")
            val industryGrowthCorrelation: Map<Long, Double>,
            @Field("emerging_trends")
            val emergingTrends: List<EmergingTrend>,
            @Field("market_maturity")
            val marketMaturity: Map<Long, String>,
        ) {
            data class EmergingTrend(
                val trend: String,
                val impactScore: Double,
                val affectedCategories: List<Long>,
            )
        }
    }

    data class JobCategoryGrowthRankingEntry(
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
        @Field("growth_metrics")
        val growthMetrics: GrowthMetrics,
        @Field("future_outlook")
        val futureOutlook: FutureOutlook,
    ) : SimpleRankingEntry {
        data class GrowthMetrics(
            @Field("posting_growth")
            val postingGrowth: Double,
            @Field("salary_growth")
            val salaryGrowth: Double,
            @Field("company_adoption")
            val companyAdoption: Double,
            @Field("skill_evolution")
            val skillEvolution: Double,
        )

        data class FutureOutlook(
            @Field("growth_sustainability")
            val growthSustainability: Double,
            @Field("market_potential")
            val marketPotential: Double,
            @Field("risk_factors")
            val riskFactors: List<String>,
            @Field("opportunity_score")
            val opportunityScore: Double,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(
            rankings.all {
                it.growthMetrics.postingGrowth >= -100.0
            },
        ) { "Growth rate cannot be less than -100%" }
    }
}
