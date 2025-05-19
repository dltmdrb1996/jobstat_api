package com.example.jobstat.core.core_mongo_base.model

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
            "시작일($startDate)은 종료일($endDate)보다 이전이거나 같아야 합니다"
        }
    }

    val durationInDays: Long
        get() = ChronoUnit.DAYS.between(startDate, endDate)
}
