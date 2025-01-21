package com.example.jobstat.core.state

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Salary(
    @Column(name = "salary_min")
    val min: Int,
    @Column(name = "salary_max")
    val max: Int,
    @Column(name = "salary_avg")
    val avg: String?,
) {
    init {
        require(min <= max) { "Minimum salary must be less than or equal to maximum salary" }
    }
}
