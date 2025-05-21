package com.example.jobstat.statistics_read.rankings.model

import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
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
