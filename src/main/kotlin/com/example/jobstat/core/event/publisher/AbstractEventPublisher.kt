package com.example.jobstat.core.event.publisher

import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.outbox.OutboxEventPublisher

abstract class AbstractEventPublisher(
    protected val outboxEventPublisher: OutboxEventPublisher
) : EventPublisher {
    
    /**
     * 이벤트 발행 구현
     * 지원하는 이벤트 타입인지 검증 후 발행
     */
    override fun publish(type: EventType, payload: EventPayload, sharedKey: Long) {
        validateEventType(type)
        outboxEventPublisher.publish(type, payload, sharedKey)
    }
    
    /**
     * 지원하는 이벤트 타입인지 검증
     * 지원하지 않는 이벤트 타입이면 예외 발생
     */
    private fun validateEventType(type: EventType) {
        require(type in getSupportedEventTypes()) {
            "Event type $type is not supported by ${this::class.simpleName}"
        }
    }
}