package com.example.jobstat.core.state

enum class CompanySize {
    MICRO,
    SMALL,
    MEDIUM,
    LARGE,
    ENTERPRISE,
    UNKNOWN,
}

fun CompanySize.getCostFactor(): Double =
    when (this) {
        CompanySize.ENTERPRISE -> 1.5
        CompanySize.LARGE -> 1.3
        CompanySize.MEDIUM -> 1.0
        CompanySize.SMALL -> 0.8
        CompanySize.MICRO -> 0.6
        CompanySize.UNKNOWN -> 0.8
    }
