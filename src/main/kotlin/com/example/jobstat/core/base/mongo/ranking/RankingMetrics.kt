package com.example.jobstat.core.base.mongo.ranking

import java.io.Serializable

interface RankingMetrics : Serializable {
    val totalCount: Int // 전체 수
    val rankedCount: Int // 순위가 매겨진 수
    val newEntries: Int // 신규 진입 수
    val droppedEntries: Int // 제외된 수
    val volatilityMetrics: VolatilityMetrics // 변동성 지표
}
