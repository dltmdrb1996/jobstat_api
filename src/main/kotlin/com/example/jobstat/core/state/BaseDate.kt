package com.example.jobstat.core.state

class BaseDate(
    private val value: String,
) {
    init {
        require(value.matches("""^\d{6}$""".toRegex())) {
            "yearMonth must be in the format 'yyyyMM'"
        }
    }

    fun toInt(): Int = value.toInt()

    override fun toString(): String = value
}
