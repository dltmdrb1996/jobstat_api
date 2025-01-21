package com.example.jobstat.core.base.mongo

import org.springframework.data.annotation.Transient

open class CommonDistribution(
    @Transient
    override val count: Int,
    @Transient
    override val ratio: Double = 0.0,
    @Transient
    override val avgSalary: Long? = null,
) : Distribution
