package com.example.jobstat.core.base.mongo.ranking

import com.example.jobstat.core.base.mongo.BaseTimeSeriesDocument
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes

@CompoundIndexes(
    CompoundIndex(
        name = "rank_lookup_idx",
        def = "{'base_date': -1, 'rankings.rank': -1}",
    ),
)
abstract class BaseRankingDocument<T : RankingEntry>(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Transient
    open val metrics: RankingMetrics,
    @Transient
    open val rankings: List<T>,
) : BaseTimeSeriesDocument(id, baseDate, period)
