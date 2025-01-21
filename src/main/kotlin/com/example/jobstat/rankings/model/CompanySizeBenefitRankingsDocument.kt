package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.DistributionRankingDocument
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.state.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_size_benefit_rankings")
class CompanySizeBenefitRankingsDocument(
    id: String? = null,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanySizeBenefitMetrics,
    @Field("group_entity_type")
    override val groupEntityType: EntityType = EntityType.COMPANY_SIZE,
    @Field("target_entity_type")
    override val targetEntityType: EntityType = EntityType.BENEFIT,
    @Field("rankings")
    override val rankings: List<CompanySizeBenefitRankingEntry>,
) : DistributionRankingDocument<CompanySizeBenefitRankingsDocument.CompanySizeBenefitRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        groupEntityType,
        targetEntityType,
        rankings,
    ) {
    data class CompanySizeBenefitMetrics(
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
        @Field("benefit_trends")
        val benefitTrends: BenefitTrends,
    ) : RankingMetrics {
        data class BenefitTrends(
            @Field("overall_distribution")
            val overallDistribution: Map<String, Double>,
            @Field("year_over_year_changes")
            val yearOverYearChanges: Map<String, Double>,
            @Field("market_comparison")
            val marketComparison: Map<String, Double>,
            @Field("industry_patterns")
            val industryPatterns: List<IndustryPattern>,
        ) {
            data class IndustryPattern(
                @Field("industry_id")
                val industryId: Long,
                @Field("industry_name")
                val industryName: String,
                @Field("distribution")
                val distribution: Map<String, Double>,
            )
        }
    }

    data class CompanySizeBenefitRankingEntry(
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
        @Field("distribution")
        override val distribution: Map<String, Double>,
        @Field("dominant_category")
        override val dominantCategory: String,
        @Field("distribution_metrics")
        override val distributionMetrics: DistributionMetrics,
        @Field("total_postings")
        val totalPostings: Int,
        @Field("benefit_metrics")
        val benefitMetrics: BenefitMetrics,
        @Field("category_distribution")
        val categoryDistribution: Map<String, BenefitCategory>,
        @Field("trend_indicators")
        val trendIndicators: TrendIndicators,
    ) : DistributionRankingEntry {
        data class BenefitMetrics(
            @Field("provision_rate")
            val provisionRate: Double,
            @Field("satisfaction_score")
            val satisfactionScore: Double,
            @Field("monetary_value")
            val monetaryValue: Long,
            @Field("benefit_count")
            val benefitCount: Int,
        )

        data class BenefitCategory(
            val provisionRate: Double,
            val importance: Double,
            val satisfactionScore: Double,
        )

        data class TrendIndicators(
            @Field("growth_rate")
            val growthRate: Double,
            @Field("change_velocity")
            val changeVelocity: Double,
            @Field("stability_score")
            val stabilityScore: Double,
            @Field("future_projection")
            val futureProjection: String,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(
            rankings.all {
                it.distribution.values.sum() in 0.99..1.01
            },
        ) { "Distribution percentages must sum to approximately 100%" }
        require(
            rankings.all {
                it.benefitMetrics.provisionRate in 0.0..1.0
            },
        ) { "Provision rate must be between 0 and 1" }
    }
}
