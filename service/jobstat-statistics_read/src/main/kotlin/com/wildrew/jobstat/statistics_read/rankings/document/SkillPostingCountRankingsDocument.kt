package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "skill_posting_count_rankings")
class SkillPostingCountRankingsDocument(
    id: String? = null,
    page : Int = 1,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: SkillPostingMetrics,
    @Field("rankings")
    override var rankings: List<SkillPostingRankingEntry>,
) : SimpleRankingDocument<SkillPostingCountRankingsDocument.SkillPostingRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page
    ) {

    data class SkillPostingMetrics(
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
    ) : RankingMetrics

    data class SkillPostingRankingEntry(
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
        @Field("value_change")
        override val valueChange: Double = 0.0,
        @Field("total_postings")
        val totalPostings: Int,
        @Field("active_postings")
        val activePostings: Int,
        @Field("posting_trend")
        val postingTrend: PostingTrend,
    ) : SimpleRankingEntry {
        data class PostingTrend(
            @Field("month_over_month_change")
            val monthOverMonthChange: Double,
            @Field("year_over_year_change")
            val yearOverYearChange: Double,
            @Field("seasonality_index")
            val seasonalityIndex: Double,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.totalPostings >= it.activePostings }) { "Active postings cannot exceed total postings" }
    }
}
