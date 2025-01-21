package com.example.jobstat.core.base.mongo

import org.springframework.data.annotation.Transient

abstract class BaseTimeSeriesDocument(
    id: String? = null,
    @Transient
    open val baseDate: String,
    @Transient
    open val period: SnapshotPeriod,
) : BaseDocument(id)