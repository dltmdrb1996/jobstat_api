package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model

import java.io.Serializable

interface Distribution : Serializable {
    val count: Int // 개수
    val ratio: Double // 비율
    val avgSalary: Long? // 평균 급여
}
