package com.example.jobstat.core.core_mongo_base.model.stats

import java.io.Serializable

interface RankingInfo : Serializable {
    val currentRank: Int
    val previousRank: Int?
    val rankChange: Int?
    val percentile: Double?
    val rankingScore: RankingScore
}
