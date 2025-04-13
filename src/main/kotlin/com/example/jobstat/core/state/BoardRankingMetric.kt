package com.example.jobstat.core.state

enum class BoardRankingMetric {
    LIKES, VIEWS;

    companion object {
        fun fromString(param: String): BoardRankingMetric {
            return entries.firstOrNull { it.name.equals(param, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid metric: $param")
        }
    }
}

enum class BoardRankingPeriod {
    DAY, WEEK, MONTH;
    fun toParamString(): String = this.name.lowercase()

    companion object {
        fun fromString(param: String): BoardRankingPeriod {
            return entries.firstOrNull { it.name.equals(param, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid period: $param")
        }
    }
}