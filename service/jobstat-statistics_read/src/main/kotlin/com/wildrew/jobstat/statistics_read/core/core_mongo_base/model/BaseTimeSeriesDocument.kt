package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model


abstract class BaseTimeSeriesDocument(
    id: String? = null,
    @org.springframework.data.annotation.Transient
    open val baseDate: String,
    @org.springframework.data.annotation.Transient
    open val period: SnapshotPeriod,
) : BaseDocument(id)

