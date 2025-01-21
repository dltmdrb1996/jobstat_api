package com.example.jobstat.core.base.mongo.ranking

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import org.springframework.data.annotation.Transient

abstract class SimpleRankingDocument<T : SimpleRankingDocument.SimpleRankingEntry>(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Transient
    override val metrics: RankingMetrics,
    @Transient
    override val rankings: List<T>,
) : BaseRankingDocument<T>(id, baseDate, period, metrics, rankings) {
    interface SimpleRankingEntry : RankingEntry {
        val score: Double
    }
}
