package com.example.jobstat.core.utils

import java.time.YearMonth
import java.time.format.DateTimeFormatter

object StatisticsCalculationUtil {
    fun calculateGrowthPercentage(oldValue: Int, newValue: Int): Double {
        if (oldValue <= 0) return 0.0
        return ((newValue - oldValue).toDouble() / oldValue) * 100.0
    }

    fun calculateDistributionRatio(count: Int, total: Int): Double {
        if (total <= 0) return 0.0
        return (count.toDouble() / total) * 100.0
    }

    fun calculatePercentile(value: Double, allValues: List<Double>): Double {
        if (allValues.isEmpty()) return 0.0
        val sortedValues = allValues.sorted()
        val index = sortedValues.binarySearch(value)
        return (index.toDouble() / allValues.size) * 100.0
    }

    private val YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")

    fun calculateLastMonthDate(): String {
        return YearMonth.now()
            .minusMonths(1)
            .format(YEAR_MONTH_FORMAT)
    }

    fun calculateLastYearDate(): String {
        return YearMonth.now()
            .minusYears(1)
            .format(YEAR_MONTH_FORMAT)
    }
}