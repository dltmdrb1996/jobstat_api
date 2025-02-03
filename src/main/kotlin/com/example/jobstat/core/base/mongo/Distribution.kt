package com.example.jobstat.core.base.mongo

import java.io.Serializable

interface Distribution : Serializable {
    val count: Int
    val ratio: Double
    val avgSalary: Long?
}
