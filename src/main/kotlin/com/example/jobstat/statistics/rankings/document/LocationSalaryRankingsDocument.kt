package com.example.jobstat.statistics.rankings.document

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "location_salary_rankings")
class LocationSalaryRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: LocationSalaryMetrics,
    @Field("rankings")
    override val rankings: List<LocationSalaryRankingEntry>,
) : SimpleRankingDocument<LocationSalaryRankingsDocument.LocationSalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class LocationSalaryMetrics(
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
        @Field("location_metrics")
        val locationMetrics: LocationMetrics,
    ) : RankingMetrics {
        data class LocationMetrics(
            @Field("cost_of_living_analysis")
            val costOfLivingAnalysis: CostOfLivingAnalysis,
            @Field("industry_distribution")
            val industryDistribution: Map<Long, Double>,
            @Field("remote_work_impact")
            val remoteWorkImpact: RemoteWorkImpact,
        ) {
            data class CostOfLivingAnalysis(
                val avgCostIndex: Double,
                val salaryAdjustmentFactor: Double,
                val livingExpenses: Map<String, Double>,
            )

            data class RemoteWorkImpact(
                val remoteJobRatio: Double,
                val salaryDifferential: Double,
                val locationFlexibility: Double,
            )
        }
    }

    data class LocationSalaryRankingEntry(
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
        @Field("salary_metrics")
        val salaryMetrics: SalaryMetrics,
        @Field("location_factors")
        val locationFactors: LocationFactors,
    ) : SimpleRankingEntry {
        data class SalaryMetrics(
            @Field("nominal_avg_salary")
            val nominalAvgSalary: Long,
            @Field("adjusted_avg_salary")
            val adjustedAvgSalary: Long,
            @Field("salary_range")
            val salaryRange: SalaryRange,
            @Field("salary_growth")
            val salaryGrowth: Double,
        ) {
            data class SalaryRange(
                val min: Long,
                val max: Long,
                val p25: Long,
                val p75: Long,
            )
        }

        data class LocationFactors(
            @Field("cost_of_living_index")
            val costOfLivingIndex: Double,
            @Field("job_market_health")
            val jobMarketHealth: Double,
            @Field("quality_of_life")
            val qualityOfLife: Double,
            @Field("career_opportunity")
            val careerOpportunity: Double,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.salaryMetrics.nominalAvgSalary > 0 }) { "Nominal salary must be positive" }
        require(rankings.all { it.salaryMetrics.adjustedAvgSalary > 0 }) { "Adjusted salary must be positive" }
    }
}
