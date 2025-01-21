package com.example.jobstat.core.base

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class JobPreferences(
    @Column(name = "desired_industries")
    val desiredIndustries: String, // Comma-separated list

    @Column(name = "desired_job_categories")
    val desiredJobCategories: String, // Comma-separated list

    @Embedded
    val desiredSalaryRange: SalaryRange,

    @Column(name = "desired_locations")
    val desiredLocations: String, // Comma-separated list

    @Column(name = "remote_work_preference")
    val remoteWorkPreference: Boolean
)

@Embeddable
data class SalaryRange(
    @Column(name = "min_salary")
    val min: Int,

    @Column(name = "max_salary")
    val max: Int
)