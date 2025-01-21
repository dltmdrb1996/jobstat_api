package com.example.jobstat.core.base.mongo.stats

interface BaseStats {
    val postingCount: Int
    val activePostingCount: Int
    val avgSalary: Long
    val growthRate: Double
    val yearOverYearGrowth: Double?
    val monthOverMonthChange: Double?
    val demandTrend: String
}
