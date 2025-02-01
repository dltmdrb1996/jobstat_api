package com.example.jobstat.rankings.document

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "skill_growth_rankings")
class SkillGrowthRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: SkillGrowthMetrics,
    @Field("rankings")
    override val rankings: List<SkillGrowthRankingEntry>,
) : SimpleRankingDocument<SkillGrowthRankingsDocument.SkillGrowthRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class SkillGrowthMetrics(
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
        @Field("growth_analysis")
        val growthAnalysis: GrowthAnalysis,
    ) : RankingMetrics {
        data class GrowthAnalysis(
            @Field("avg_growth_rate")
            val avgGrowthRate: Double,
            @Field("median_growth_rate")
            val medianGrowthRate: Double,
            @Field("growth_distribution")
            val growthDistribution: Map<String, Int>,
        )
    }

    data class SkillGrowthRankingEntry(
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
        @Field("growth_rate")
        val growthRate: Double,
        @Field("growth_consistency")
        val growthConsistency: Double,
        @Field("growth_factors")
        val growthFactors: GrowthFactors,
    ) : SimpleRankingEntry {
        data class GrowthFactors(
            @Field("demand_growth")
            val demandGrowth: Double,
            @Field("salary_growth")
            val salaryGrowth: Double,
            @Field("adoption_rate")
            val adoptionRate: Double,
            @Field("market_penetration")
            val marketPenetration: Double,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.growthRate >= -100.0 }) { "Growth rate cannot be less than -100%" }
    }

    fun copy(
        id: String? = this.id,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        metrics: SkillGrowthMetrics = this.metrics,
        rankings: List<SkillGrowthRankingEntry> = this.rankings,
        page: Int = this.page,
    ): SkillGrowthRankingsDocument = SkillGrowthRankingsDocument(id, page, baseDate, period, metrics, rankings)
}
