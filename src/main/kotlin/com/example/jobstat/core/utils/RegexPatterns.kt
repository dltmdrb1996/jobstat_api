package com.example.jobstat.core.utils

object RegexPatterns {
    const val USERNAME_PATTERN = "^[가-힣a-zA-Z0-9]{3,10}$"
    const val EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"

    val USERNAME = USERNAME_PATTERN.toRegex()
    val EMAIL = EMAIL_PATTERN.toRegex()
}
