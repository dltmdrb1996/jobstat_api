package com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking

import java.io.Serializable

interface RankingEntry : Serializable {
    val entityId: Long
    val name: String
    val rank: Int
    val previousRank: Int?
    val rankChange: Int?
}
