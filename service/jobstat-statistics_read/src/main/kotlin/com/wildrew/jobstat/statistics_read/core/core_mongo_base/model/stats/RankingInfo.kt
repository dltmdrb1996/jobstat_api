package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats

import java.io.Serializable

interface RankingInfo {
    val currentRank: Int
    val previousRank: Int?
    val rankChange: Int?
    val percentile: Double?
    val valueChange: Double?
    val rankingScore: RankingScore
}