package com.example.jobstat.statistics_read.rankings.document

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "location_posting_count_rankings")
class LocationPostingCountRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: LocationPostingMetrics,
    @Field("rankings")
    override val rankings: List<LocationPostingRankingEntry>,
) : SimpleRankingDocument<LocationPostingCountRankingsDocument.LocationPostingRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class LocationPostingMetrics(
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
        @Field("market_metrics")
        val marketMetrics: MarketMetrics,
    ) : RankingMetrics {
        data class MarketMetrics(
            @Field("job_market_analysis")
            val jobMarketAnalysis: JobMarketAnalysis,
            @Field("employment_stats")
            val employmentStats: EmploymentStats,
            @Field("geographic_distribution")
            val geographicDistribution: Map<String, Double>,
        ) {
            data class JobMarketAnalysis(
                val marketDynamics: String,
                val competitionLevel: Double,
                val marketGrowth: Double,
            )

            data class EmploymentStats(
                val employmentRate: Double,
                val laborForceParticipation: Double,
                val unemploymentRate: Double,
            )
        }
    }

    data class LocationPostingRankingEntry(
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
        @Field("posting_stats")
        val postingStats: PostingStats,
        @Field("market_indicators")
        val marketIndicators: MarketIndicators,
    ) : SimpleRankingEntry {
        data class PostingStats(
            @Field("total_postings")
            val totalPostings: Int,
            @Field("active_postings")
            val activePostings: Int,
            @Field("postings_by_type")
            val postingsByType: Map<String, Int>,
            @Field("remote_postings")
            val remotePostings: Int,
        )

        data class MarketIndicators(
            @Field("job_density")
            val jobDensity: Double,
            @Field("market_saturation")
            val marketSaturation: Double,
            @Field("opportunity_index")
            val opportunityIndex: Double,
            @Field("growth_potential")
            val growthPotential: Double,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(
            rankings.all {
                it.postingStats.totalPostings >= it.postingStats.activePostings
            },
        ) { "활성 채용공고 수는 전체 채용공고 수를 초과할 수 없습니다" }
    }
}
