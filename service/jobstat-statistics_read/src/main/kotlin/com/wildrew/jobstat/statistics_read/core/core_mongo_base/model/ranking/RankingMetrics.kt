package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking

import java.io.Serializable

interface RankingMetrics : Serializable {
    val totalCount: Int
    val rankedCount: Int
    val newEntries: Int
    val droppedEntries: Int
    val volatilityMetrics: VolatilityMetrics
}
