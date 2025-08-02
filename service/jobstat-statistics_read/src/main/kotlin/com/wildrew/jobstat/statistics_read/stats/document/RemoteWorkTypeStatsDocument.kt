package com.wildrew.jobstat.statistics_read.stats.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.CommonStats
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingInfo
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingScore
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "remote_work_type_stats_monthly")
class RemoteWorkTypeStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: RemoteWorkTypeStats,
    @Field("rankings")
    override val rankings: Map<RankingType, RemoteWorkTypeRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class RemoteWorkTypeStats(
        @Field("posting_count")
        override val postingCount: Int,
        @Field("active_posting_count")
        override val activePostingCount: Int,
        @Field("avg_salary")
        override val avgSalary: Long,
        @Field("growth_rate")
        override val growthRate: Double,
        @Field("year_over_year_growth")
        override val yearOverYearGrowth: Double?,
        @Field("month_over_month_change")
        override val monthOverMonthChange: Double?,
        @Field("demand_trend")
        override val demandTrend: String,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class RemoteWorkTypeRankingInfo(
        @Field("current_rank")
        override val currentRank: Int,
        @Field("previous_rank")
        override val previousRank: Int?,
        @Field("rank_change")
        override val rankChange: Int?,
        @Field("percentile")
        override val percentile: Double?,
        @Field("ranking_score")
        override val rankingScore: RankingScore,
        @Field("value_change")
        override val valueChange: Double?,
    ) : RankingInfo

    override fun validate() {
        TODO("Not yet implemented")
    }

    fun copy(
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        name: String = this.name,
        stats: RemoteWorkTypeStats = this.stats,
        rankings: Map<RankingType, RemoteWorkTypeRankingInfo> = this.rankings,
    ) = RemoteWorkTypeStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        name = name,
        stats = stats,
        rankings = rankings,
    )
}
