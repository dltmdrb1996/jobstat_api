package com.example.jobstat.core.global.utils

object RegexPatterns {
    object Patterns {
        const val USERNAME = "^[가-힣a-zA-Z0-9]{3,15}$"
        const val EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    }

    val USERNAME = Patterns.USERNAME.toRegex()
    val EMAIL = Patterns.EMAIL.toRegex()
}
