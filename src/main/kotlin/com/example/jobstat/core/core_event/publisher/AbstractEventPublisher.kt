package com.example.jobstat.core.core_event.publisher

import com.example.jobstat.core.core_event.model.EventPayload
import com.example.jobstat.core.core_event.model.EventType
import com.example.jobstat.core.core_event.outbox.OutboxEventPublisher
import org.slf4j.LoggerFactory

abstract class AbstractEventPublisher(
    protected val outboxEventPublisher: OutboxEventPublisher,
) : EventPublisher {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.debug(
            "[{}] 이벤트 발행 시도: type=$type, payloadType=${payload::class.simpleName}",
            this::class.simpleName,
        )

        try {
            validateEventType(type)
            log.debug("[{}] 이벤트 타입 검증 성공: type=$type", this::class.simpleName)

            outboxEventPublisher.publish(type, payload)
            log.debug("[{}] 이벤트 발행 요청 완료: type=$type", this::class.simpleName)
        } catch (e: Exception) {
            log.error(
                "[{}] 이벤트 발행 중 오류 발생: type=$type, error=${e.message}",
                this::class.simpleName,
                e,
            )
            throw e
        }
    }

    private fun validateEventType(type: EventType) {
        val supportedTypes = getSupportedEventTypes()
        log.debug("[{}] 지원하는 이벤트 타입 목록: {}", this::class.simpleName, supportedTypes)

        require(type in supportedTypes) {
            log.warn(
                "[{}] 지원하지 않는 이벤트 타입: type={}, supported={}",
                this::class.simpleName,
                type,
                supportedTypes,
            )
            "이벤트 타입 $type 은 ${this::class.simpleName}에서 지원되지 않습니다."
        }
    }
}
