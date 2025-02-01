package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.base.mongo.ranking.RankingType
import java.io.Serializable

data class RankingPage(
    val items: RankingData,
    val hasNextPage: Boolean,
    val nextCursor: Int?,
) : Serializable

data class RankingData(
    val type: RankingType,
    val data: List<RankingEntry>,
)
