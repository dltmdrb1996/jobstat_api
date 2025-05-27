package com.wildrew.jobstat.core.core_event.model

data class ConsumerConfig(
    var topic: String? = null,
    var groupId: String? = null,
    var retry: RetryConfig? = RetryConfig(),
)
