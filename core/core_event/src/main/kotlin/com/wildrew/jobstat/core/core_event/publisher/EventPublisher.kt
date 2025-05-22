package com.wildrew.jobstat.core.core_event.publisher

import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.EventType

interface EventPublisher {
    fun publish(
        type: EventType,
        payload: EventPayload,
    )

    fun getSupportedEventTypes(): Set<EventType>
}
