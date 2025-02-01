package com.example.jobstat.rankings.model

data class RankingSummary(
    val topPerformers: List<RankingAnalysis<*>>,
    val trends: RankingTrends,
)
