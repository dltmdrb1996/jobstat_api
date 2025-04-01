package com.example.jobstat.core.event.outbox

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.global.utils.Snowflake
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class OutboxEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dataSerializer: DataSerializer,
) {
    private val outboxIdGenerator = Snowflake()
    private val eventIdGenerator = Snowflake()

    fun publish(type: EventType, payload: EventPayload, sharedKey: Long) {
        val outbox = Outbox.create(
            outboxId = outboxIdGenerator.nextId(),
            eventType = type,
            payload = Event.of(
                eventId = eventIdGenerator.nextId(),
                type = type,
                payload = payload
            ).toJson(dataSerializer),
            shardKey = sharedKey and OutboxConstants.SHARD_COUNT.toLong()
        )
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox))
    }
}