package com.example.jobstat.core.core_mongo_base.model.stats
import org.springframework.data.annotation.Transient

open class CommonStats(
    @Transient
    override val postingCount: Int = 0,
    @Transient
    override val activePostingCount: Int = 0,
    @Transient
    override val avgSalary: Long = 0,
    @Transient
    override val growthRate: Double = 0.0,
    @Transient
    override val yearOverYearGrowth: Double? = null,
    @Transient
    override val monthOverMonthChange: Double? = null,
    @Transient
    override val demandTrend: String = "STABLE",
) : BaseStats
