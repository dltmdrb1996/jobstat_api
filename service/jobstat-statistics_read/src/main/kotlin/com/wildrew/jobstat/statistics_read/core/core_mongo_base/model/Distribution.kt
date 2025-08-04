package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model

import java.io.Serializable

interface Distribution : Serializable {
    val count: Int
    val ratio: Double
    val avgSalary: Long?
}
