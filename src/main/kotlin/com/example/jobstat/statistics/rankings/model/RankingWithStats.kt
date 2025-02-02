package com.example.jobstat.statistics.rankings.model

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument

data class RankingWithStats<T : BaseStatsDocument>(
    val ranking: RankingEntry,
    val stats: T,
)
