package com.wildrew.jobstat.statistics_read.rankings.model

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry

data class PureRankingPage(
    val items: List<RankingEntry>,
    val totalCount: Int,
    val hasNextPage: Boolean,
    val nextCursor: Int?,
)
