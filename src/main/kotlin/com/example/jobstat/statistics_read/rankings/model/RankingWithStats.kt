package com.example.jobstat.statistics_read.rankings.model

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument

data class RakingWithStatsPage<T : BaseStatsDocument>(
    val items: List<RankingWithStats<T>>,
    val totalCount: Int,
    val hasNextPage: Boolean,
)

data class RankingWithStats<T : Any>(
    val ranking: RankingEntry,
    val stat: T,
)
