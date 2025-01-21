package com.example.jobstat.core.base.mongo.ranking

interface RankingMetrics {
    val totalCount: Int
    val rankedCount: Int
    val newEntries: Int
    val droppedEntries: Int
    val volatilityMetrics: VolatilityMetrics
}
