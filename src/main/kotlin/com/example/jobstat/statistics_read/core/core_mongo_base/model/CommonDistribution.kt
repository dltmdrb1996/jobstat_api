package com.example.jobstat.statistics_read.core.core_mongo_base.model

import org.springframework.data.annotation.Transient

open class CommonDistribution(
    @Transient
    override val count: Int,
    @Transient
    override val ratio: Double = 0.0,
    @Transient
    override val avgSalary: Long? = null,
) : Distribution
