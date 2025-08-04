package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseTimeSeriesDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes

@CompoundIndexes(
    CompoundIndex(
        name = "snapshot_lookup_idx",
        def = "{'entity_id': 1, 'base_date': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "date_entity_idx",
        def = "{'base_date': 1, 'entity_id': 1}",
    ),
    CompoundIndex(
        name = "entity_latest_idx",
        def = "{'entity_id': 1, 'base_date': -1}",
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
