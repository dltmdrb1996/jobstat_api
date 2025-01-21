package com.example.jobstat.core.base.mongo.ranking

import org.springframework.data.mongodb.core.mapping.Field

data class VolatilityMetrics(
    @Field("avg_rank_change")
    val avgRankChange: Double,
    @Field("rank_change_std_dev")
    val rankChangeStdDev: Double,
    @Field("volatility_trend")
    val volatilityTrend: String,
)
