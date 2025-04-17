package com.example.jobstat.core.event.publisher

import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType

interface EventPublisher {
    fun publish(
        type: EventType,
        payload: EventPayload,
    )

    fun getSupportedEventTypes(): Set<EventType>
}
