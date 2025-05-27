package com.wildrew.jobstat.statistics_read.rankings.model

data class RankingTrends(
    val topMovers: List<RankingAnalysis<*>>,
    val topLosers: List<RankingAnalysis<*>>,
    val volatileEntities: List<RankingAnalysis<*>>,
    val consistentEntities: List<RankingAnalysis<*>>,
)
