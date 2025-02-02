package com.example.jobstat.core.base.mongo.stats

import com.example.jobstat.core.base.mongo.BaseTimeSeriesDocument
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.statistics.rankings.model.RankingType
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes

@CompoundIndexes(
    CompoundIndex(
        name = "snapshot_lookup_idx",
        def = "{'entity_id': 1, 'base_date': 1}",
        unique = true,
    ),
)
abstract class BaseStatsDocument(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Transient
    open val entityId: Long,
    @Transient
    open val stats: BaseStats,
    @Transient
    open val rankings: Map<RankingType, RankingInfo>,
) : BaseTimeSeriesDocument(id, baseDate, period)
