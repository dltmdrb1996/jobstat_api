package com.example.jobstat.core.base.mongo

import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit

data class SnapshotPeriod(
    @Field("start_date")
    val startDate: Instant,
    @Field("end_date")
    val endDate: Instant,
) : Serializable {
    init {
        require(!startDate.isAfter(endDate)) {
            "Start date ($startDate) must be before or equal to end date ($endDate)"
        }
    }

    val durationInDays: Long
        get() = ChronoUnit.DAYS.between(startDate, endDate)
}
