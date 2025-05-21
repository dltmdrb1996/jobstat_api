package com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking

import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

data class VolatilityMetrics(
    @Field("avg_rank_change")
    val avgRankChange: Double,
    @Field("rank_change_std_dev")
    val rankChangeStdDev: Double,
    @Field("volatility_trend")
    val volatilityTrend: String,
) : Serializable
