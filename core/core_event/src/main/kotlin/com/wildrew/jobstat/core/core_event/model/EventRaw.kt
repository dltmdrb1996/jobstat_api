package com.wildrew.jobstat.core.core_event.model

data class EventRaw(
    val eventId: Long,
    val type: String,
    val payload: Any,
)
