package com.example.jobstat.statistics.rankings.model

data class RankingSummary(
    val topPerformers: List<RankingAnalysis<*>>,
    val trends: RankingTrends,
)
