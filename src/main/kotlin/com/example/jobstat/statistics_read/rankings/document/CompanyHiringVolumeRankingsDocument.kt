package com.example.jobstat.statistics_read.rankings.document

import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.core.core_mongo_base.model.ranking.RankingMetrics
import com.example.jobstat.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.example.jobstat.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_hiring_volume_rankings")
class CompanyHiringVolumeRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanyHiringMetrics,
    @Field("rankings")
    override val rankings: List<CompanyHiringRankingEntry>,
) : SimpleRankingDocument<CompanyHiringVolumeRankingsDocument.CompanyHiringRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class CompanyHiringMetrics(
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
        @Field("hiring_metrics")
        val hiringMetrics: HiringMetrics,
    ) : RankingMetrics {
        data class HiringMetrics(
            @Field("hiring_velocity")
            val hiringVelocity: HiringVelocity,
            @Field("company_size_distribution")
            val companySizeDistribution: Map<String, HiringStats>,
            @Field("industry_patterns")
            val industryPatterns: Map<Long, HiringTrend>,
        ) {
            data class HiringVelocity(
                val avgTimeToHire: Double,
                val positionFillRate: Double,
                val hiringEfficiency: Double,
            )

            data class HiringStats(
                val totalHires: Int,
                val growthRate: Double,
                val avgPositionsOpen: Int,
            )

            data class HiringTrend(
                val hiringVolume: Int,
                val growthRate: Double,
                val marketShare: Double,
            )
        }
    }

    data class CompanyHiringRankingEntry(
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
        @Field("hiring_details")
        val hiringDetails: HiringDetails,
        @Field("growth_indicators")
        val growthIndicators: GrowthIndicators,
    ) : SimpleRankingEntry {
        data class HiringDetails(
            @Field("total_positions")
            val totalPositions: Int,
            @Field("filled_positions")
            val filledPositions: Int,
            @Field("hiring_by_department")
            val hiringByDepartment: Map<String, Int>,
            @Field("hiring_timeline")
            val hiringTimeline: HiringTimeline,
        ) {
            data class HiringTimeline(
                val avgTimeToHire: Double,
                val timeToOfferAcceptance: Double,
                val onboardingTime: Double,
            )
        }

        data class GrowthIndicators(
            @Field("hiring_growth_rate")
            val hiringGrowthRate: Double,
            @Field("retention_rate")
            val retentionRate: Double,
            @Field("expansion_rate")
            val expansionRate: Double,
            @Field("workforce_planning")
            val workforcePlanning: WorkforcePlanning,
        ) {
            data class WorkforcePlanning(
                val shortTermNeeds: Int,
                val longTermProjections: Int,
                val skillGaps: List<String>,
            )
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(
            rankings.all {
                it.hiringDetails.totalPositions >= it.hiringDetails.filledPositions
            },
        ) { "채용된 포지션 수는 전체 포지션 수를 초과할 수 없습니다" }
    }
}
