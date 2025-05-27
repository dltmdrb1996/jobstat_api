package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
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
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(rankings.all { it.salaryMetrics.nominalAvgSalary > 0 }) { "명목 평균 급여는 양수여야 합니다" }
        require(rankings.all { it.salaryMetrics.adjustedAvgSalary > 0 }) { "조정된 평균 급여는 양수여야 합니다" }
    }
}
