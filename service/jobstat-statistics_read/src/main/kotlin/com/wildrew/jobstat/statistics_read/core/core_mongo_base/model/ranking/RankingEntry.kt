package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking

interface RankingEntry {
    val documentId: String
    val entityId: Long
    val name: String
    val rank: Int
    val previousRank: Int?
    val valueChange: Double
    val rankChange: Int?
}
