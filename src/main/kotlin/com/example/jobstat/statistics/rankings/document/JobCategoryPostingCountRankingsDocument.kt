package com.example.jobstat.statistics.rankings.document

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "job_category_posting_count_rankings")
class JobCategoryPostingCountRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: JobCategoryPostingMetrics,
    @Field("rankings")
    override val rankings: List<JobCategoryPostingRankingEntry>,
) : SimpleRankingDocument<JobCategoryPostingCountRankingsDocument.JobCategoryPostingRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class JobCategoryPostingMetrics(
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
        @Field("market_analysis")
        val marketAnalysis: MarketAnalysis,
    ) : RankingMetrics {
        data class MarketAnalysis(
            @Field("total_market_demand")
            val totalMarketDemand: Int,
            @Field("demand_by_location")
            val demandByLocation: Map<Long, Int>,
            @Field("company_size_distribution")
            val companySizeDistribution: Map<String, Int>,
            @Field("remote_work_ratio")
            val remoteWorkRatio: Double,
        )
    }

    data class JobCategoryPostingRankingEntry(
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
        @Field("posting_details")
        val postingDetails: PostingDetails,
        @Field("demand_indicators")
        val demandIndicators: DemandIndicators,
    ) : SimpleRankingEntry {
        data class PostingDetails(
            @Field("total_postings")
            val totalPostings: Int,
            @Field("active_postings")
            val activePostings: Int,
            @Field("posting_by_experience")
            val postingByExperience: Map<String, Int>,
            @Field("remote_work_postings")
            val remoteWorkPostings: Int,
        )

        data class DemandIndicators(
            @Field("application_rate")
            val applicationRate: Double,
            @Field("time_to_fill")
            val timeToFill: Double, // 평균 채용 소요 기간
            @Field("competition_rate")
            val competitionRate: Double, // 지원자 대비 채용 비율
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(
            rankings.all {
                it.postingDetails.totalPostings >= it.postingDetails.activePostings
            },
        ) { "Active postings cannot exceed total postings" }
    }
}
