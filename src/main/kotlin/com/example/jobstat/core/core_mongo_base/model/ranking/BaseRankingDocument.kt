package com.example.jobstat.core.core_mongo_base.model.ranking

import com.example.jobstat.core.core_mongo_base.model.BaseTimeSeriesDocument
import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Field

// 순위 정보를 저장하는 기본 문서 클래스
// 순위 조회를 위한 인덱스를 포함합니다
@CompoundIndexes(
    CompoundIndex(
        name = "rank_lookup_idx", // 순위 조회 인덱스
        def = "{'base_date': -1, 'rankings.rank': 1}",
    ),
    CompoundIndex(
        name = "base_date_page_idx",
        def = "{'base_date': 1, 'page': 1}",
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
    @Field("page")
    val page: Int,
) : BaseTimeSeriesDocument(id, baseDate, period)
