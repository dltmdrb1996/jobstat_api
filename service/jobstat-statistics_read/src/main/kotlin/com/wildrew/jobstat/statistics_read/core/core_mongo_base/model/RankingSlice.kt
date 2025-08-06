package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry

data class RankingSlice<T : RankingEntry>(
    val totalCount: Int = 0,
    val items: List<T> = emptyList(),
)
