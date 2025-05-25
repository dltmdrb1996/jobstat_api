package com.wildrew.jobstat.core.core_event.config

data class ConsumerConfig(
    var topic: String? = null,
    var groupId: String? = null,
    var retry: RetryConfig? = RetryConfig() // 기본값 설정
)

data class RetryConfig(
    var attempts: Int = 3,
    var delayMs: Long = 1000,
    var multiplier: Double = 2.0
)