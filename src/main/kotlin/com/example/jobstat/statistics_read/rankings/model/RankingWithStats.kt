package com.example.jobstat.statistics_read.rankings.model

import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.example.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument

data class RakingWithStatsPage<T : BaseStatsDocument>(
    val items: List<RankingWithStats<T>>,
    val totalCount: Int,
    val hasNextPage: Boolean,
)

data class RankingWithStats<T : Any>(
    val ranking: RankingEntry,
    val stat: T,
)
