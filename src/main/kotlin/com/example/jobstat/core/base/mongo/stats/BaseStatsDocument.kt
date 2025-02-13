package com.example.jobstat.core.base.mongo.stats

import com.example.jobstat.core.base.mongo.BaseTimeSeriesDocument
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes

// 통계 정보를 저장하는 기본 문서 클래스
// 엔티티별 스냅샷 조회를 위한 인덱스를 포함합니다
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
