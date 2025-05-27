package com.wildrew.jobstat.statistics_read.rankings.model

data class RankingSummary(
    val topPerformers: List<RankingAnalysis<*>>,
    val trends: RankingTrends,
)
