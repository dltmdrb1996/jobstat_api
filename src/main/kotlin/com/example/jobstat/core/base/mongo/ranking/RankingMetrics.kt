package com.example.jobstat.core.base.mongo.ranking

import java.io.Serializable

interface RankingMetrics : Serializable {
    val totalCount: Int
    val rankedCount: Int
    val newEntries: Int
    val droppedEntries: Int
    val volatilityMetrics: VolatilityMetrics
}
