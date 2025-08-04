package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseTimeSeriesDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Field

@CompoundIndexes(
    CompoundIndex(
        name = "rank_lookup_idx",
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
