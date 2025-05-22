package com.wildrew.jobstat.core.core_global.model

class BaseDate(
    private var value: String,
) {
    companion object {
        fun now(): BaseDate {
            val now = java.time.LocalDate.now()
            val year = now.year
            // val month = now.monthValue
            val month = 1
            val monthString = if (month < 10) "0$month" else month.toString()
            return BaseDate("$year$monthString")
        }
    }

    init {
        require(value.matches("""^\d{4}\d{2}$""".toRegex())) {
            "연월은 'YYYYMM' 형식이어야 합니다"
        }
    }

    val year by lazy { value.substring(0, 4).toInt() }
    val month by lazy { value.substring(4, 6).toInt() }

    fun plusMonths(plus: Int): BaseDate {
        val newMonth = month + plus
        val newYear = year + (newMonth - 1) / 12
        val newMonthValue = ((newMonth - 1) % 12) + 1
        val newMonthString = if (newMonthValue < 10) "0$newMonthValue" else newMonthValue.toString()
        return BaseDate("$newYear$newMonthString")
    }

    fun minusMonths(minus: Int): BaseDate {
        val totalMonths = year * 12 + month - minus
        val newYear = totalMonths / 12
        val newMonthValue = totalMonths % 12
        // 0월이 나오면 12월로 조정하고 연도 감소
        val adjustedMonth = if (newMonthValue == 0) 12 else newMonthValue
        val adjustedYear = if (newMonthValue == 0) newYear - 1 else newYear
        val newMonthString = if (adjustedMonth < 10) "0$adjustedMonth" else adjustedMonth.toString()
        return BaseDate("$adjustedYear$newMonthString")
    }

    // MongoDB 쿼리용 toString()
    override fun toString(): String = value
}
