package com.wildrew.app.statistics_read.rankings.model

import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import java.io.Serializable

data class RankingPage(
    val items: RankingData,
    val rankedCount: Int,
    val hasNextPage: Boolean,
) : Serializable

data class RankingData(
    val type: RankingType,
    val data: List<RankingEntry>,
)
