package com.example.jobstat.core.state

class BaseDate(
    private var value: String,
) {
    companion object {
        fun now(): BaseDate {
            val now = java.time.LocalDate.now()
            val year = now.year
//            val month = now.monthValue
            val month = 1
            val monthString = if (month < 10) "0$month" else month.toString()
            return BaseDate("$year$monthString")
        }
    }

    init {
        require(value.matches("""^\d{4}\d{2}$""".toRegex())) {
            "연월은 'YYYY-MM' 형식이어야 합니다"
        }
    }

    val year by lazy { value.substring(0, 4).toInt() }
    val month by lazy { value.substring(4, 6).toInt() }

    fun plusMonths(plus: Int): BaseDate {
        val newMonth = month + plus
        val newYear = year + (newMonth - 1) / 12
        val newMonthValue = ((newMonth - 1) % 12) + 1
        val newMonthString = if (newMonthValue < 10) "0$newMonthValue" else newMonthValue.toString()
        return BaseDate("$newYear-$newMonthString")
    }

    fun minusMonths(minus: Int): BaseDate {
        val (year, month) = value.split("-").map { it.toInt() }
        val newMonth = month - minus
        val yearsToSubtract = (newMonth - 1).floorDiv(12)
        val newYear = year + yearsToSubtract
        val newMonthValue = ((newMonth - 1) % 12 + 12) % 12 + 1
        val newMonthString = if (newMonthValue < 10) "0$newMonthValue" else newMonthValue.toString()
        return BaseDate("$newYear-$newMonthString")
    }

    // MongoDB 쿼리용 toString()
    override fun toString(): String = value
}
