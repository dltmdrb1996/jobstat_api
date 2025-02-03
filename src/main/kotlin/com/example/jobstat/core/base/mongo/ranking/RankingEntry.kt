package com.example.jobstat.core.base.mongo.ranking

import java.io.Serializable

interface RankingEntry : Serializable {
    val documentId: String
    val entityId: Long
    val name: String
    val rank: Int
    val previousRank: Int?
    val rankChange: Int?
}
