package com.example.jobstat.core.event

data class EventRaw(
    val eventId: Long,
    val type: String,
    val payload: Any,
)
