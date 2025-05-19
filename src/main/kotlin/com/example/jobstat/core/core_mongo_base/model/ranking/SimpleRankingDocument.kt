package com.example.jobstat.core.core_mongo_base.model.ranking

import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import org.springframework.data.annotation.Transient

abstract class SimpleRankingDocument<T : SimpleRankingDocument.SimpleRankingEntry>(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Transient
    override val metrics: RankingMetrics,
    @Transient
    override val rankings: List<T>,
    page: Int,
) : BaseRankingDocument<T>(id, baseDate, period, metrics, rankings, page) {
    interface SimpleRankingEntry : RankingEntry {
        val score: Double
    }
}
