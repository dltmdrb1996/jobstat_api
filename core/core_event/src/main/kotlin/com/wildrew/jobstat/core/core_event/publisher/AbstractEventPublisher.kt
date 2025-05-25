package com.wildrew.jobstat.core.core_event.publisher

import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.outbox.OutboxEventPublisher
import org.slf4j.LoggerFactory

abstract class AbstractEventPublisher(
    private val outboxEventPublisher: OutboxEventPublisher,
) : EventPublisher {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.debug(
            "[{}] 이벤트 발행 시도: type={}, payloadType={}",
            this::class.simpleName,
            type,
            payload::class.simpleName,
        )
        try {
            validateEventType(type)
            log.debug("[{}] 이벤트 타입 검증 통과: type={}", this::class.simpleName, type)

            outboxEventPublisher.publish(type, payload)
            log.debug("[{}] OutboxEventPublisher에 발행 요청 완료: type={}", this::class.simpleName, type)
        } catch (e: Exception) {
            log.error(
                "[{}] 이벤트 발행 중 오류 발생: type={}, error={}",
                this::class.simpleName,
                type,
                e.message,
                e,
            )
            throw e // 예외 전파
        }
    }

    // 이 메소드는 추상 클래스를 상속받는 서비스의 구체 Publisher 클래스에서 구현해야 함.
    abstract override fun getSupportedEventTypes(): Set<EventType>

    private fun validateEventType(type: EventType) {
        val supportedTypes = getSupportedEventTypes()
        log.debug("[{}] 지원 이벤트 타입: {}", this::class.simpleName, supportedTypes)

        require(type in supportedTypes) {
            val errorMessage = "이벤트 타입 $type 은(는) ${this::class.simpleName} 에서 지원되지 않습니다. 지원 타입: $supportedTypes"
            log.warn(errorMessage)
            errorMessage // require의 메시지로 사용됨
        }
    }
}
