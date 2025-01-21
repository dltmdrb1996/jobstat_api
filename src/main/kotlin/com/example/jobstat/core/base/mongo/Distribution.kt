package com.example.jobstat.core.base.mongo

interface Distribution {
    val count: Int
    val ratio: Double
    val avgSalary: Long?
}
