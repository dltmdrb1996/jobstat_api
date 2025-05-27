// package com.wildrew.jobstat.statistics_read.core.core_model
//
// import jakarta.persistence.Column
// import jakarta.persistence.Embeddable
//
// @Embeddable
// data class Salary(
//    @Column(name = "salary_min")
//    val min: Int,
//    @Column(name = "salary_max")
//    val max: Int,
//    @Column(name = "salary_avg")
//    val avg: String?,
// ) {
//    init {
//        require(min <= max) { "최소 급여는 최대 급여보다 작거나 같아야 합니다" }
//    }
// }
