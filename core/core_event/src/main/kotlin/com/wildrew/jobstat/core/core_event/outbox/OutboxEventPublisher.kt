package com.wildrew.jobstat.core.core_event.outbox

import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.EventType

interface OutboxEventPublisher {
    fun publish(
        type: EventType,
        payload: EventPayload,
    )
}
