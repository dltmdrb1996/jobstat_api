package com.wildrew.jobstat.core.core_event.model

data class RetryConfig(
    var attempts: Int = 3,
    var delayMs: Long = 1000,
    var multiplier: Double = 2.0,
)
