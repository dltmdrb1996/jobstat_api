package com.example.jobstat.core.core_mongo_base.model

import org.springframework.data.mongodb.core.mapping.Field

abstract class BaseTimeSeriesDocument(
    id: String? = null,
    @Field("base_date")
    open val baseDate: String,
    @Field("period")
    open val period: SnapshotPeriod,
) : BaseDocument(id)
