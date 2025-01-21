package com.example.jobstat.core.base.mongo.stats

interface RankingInfo {
    val currentRank: Int
    val previousRank: Int?
    val rankChange: Int?
    val percentile: Double?
    val rankingScore: RankingScore
}