package com.example.jobstat.core.state

class BaseDate(
    private val value: String,
) {
    init {
        require(value.matches("""^\d{6}$""".toRegex())) {
            "yearMonth must be in the format 'yyyyMM'"
        }
    }

    fun plusMonths(plus: Int): BaseDate {
        val year = value.substring(0, 4).toInt()
        val month = value.substring(4, 6).toInt()
        val newMonth = month + plus
        val newYear = year + newMonth / 12
        val newMonthValue = newMonth % 12
        val newMonthString = if (newMonthValue < 10) "0$newMonthValue" else newMonthValue.toString()
        return BaseDate("$newYear$newMonthString")
    }

    fun minusMonths(minus: Int): BaseDate {
        val year = value.substring(0, 4).toInt()
        val month = value.substring(4, 6).toInt()
        val newMonth = month - minus
        val newYear = year + newMonth / 12
        val newMonthValue = newMonth % 12
        val newMonthString = if (newMonthValue < 10) "0$newMonthValue" else newMonthValue.toString()
        return BaseDate("$newYear$newMonthString")
    }

    fun toInt(): Int = value.toInt()

    override fun toString(): String = value
}
