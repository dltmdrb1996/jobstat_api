package com.example.jobstat.core.base.mongo.stats

import java.io.Serializable

interface BaseStats : Serializable {
    val postingCount: Int
    val activePostingCount: Int
    val avgSalary: Long
    val growthRate: Double
    val yearOverYearGrowth: Double?
    val monthOverMonthChange: Double?
    val demandTrend: String
}
