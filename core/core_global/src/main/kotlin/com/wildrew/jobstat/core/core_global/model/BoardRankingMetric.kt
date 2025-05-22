package com.wildrew.jobstat.core.core_global.model

enum class BoardRankingMetric {
    LIKES,
    VIEWS,
    ;

    companion object {
        fun fromString(param: String): BoardRankingMetric =
            entries.firstOrNull { it.name.equals(param, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid metric: $param")
    }
}

enum class BoardRankingPeriod {
    DAY,
    WEEK,
    MONTH,
    ;

    fun toParamString(): String = this.name.lowercase()

    companion object {
        fun fromString(param: String): BoardRankingPeriod =
            entries.firstOrNull { it.name.equals(param, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid period: $param")
    }
}
