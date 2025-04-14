package com.example.jobstat.core.event.publisher

import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.outbox.OutboxEventPublisher
import org.slf4j.LoggerFactory

/**
 * 추상 이벤트 발행자
 * 공통 이벤트 발행 로직을 구현하고, 구체적인 발행자 클래스에서 재사용할 수 있게 합니다.
 */
abstract class AbstractEventPublisher(
    protected val outboxEventPublisher: OutboxEventPublisher,
) : EventPublisher {
    private val log = LoggerFactory.getLogger(this::class.java)

    // ===================================================
    // 이벤트 발행 메소드
    // ===================================================

    /**
     * 이벤트 발행 구현
     * 지원하는 이벤트 타입인지 검증 후 발행합니다.
     *
     * @param type 이벤트 타입
     * @param payload 이벤트 페이로드
     */
    override fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.info(
            "[{}] 이벤트 발행 시도: type=$type, payloadType=${payload::class.simpleName}",
            this::class.simpleName,
        )

        try {
            // 1. 이벤트 타입 검증
            validateEventType(type)
            log.info("[{}] 이벤트 타입 검증 성공: type=$type", this::class.simpleName)

            // 2. Outbox 발행자를 통해 이벤트 발행
            outboxEventPublisher.publish(type, payload)
            log.info("[{}] 이벤트 발행 요청 완료: type=$type", this::class.simpleName)
        } catch (e: Exception) {
            log.error(
                "[{}] 이벤트 발행 중 오류 발생: type=$type, error=${e.message}",
                this::class.simpleName,
                e,
            )
            throw e
        }
    }

    // ===================================================
    // 유효성 검증 메소드
    // ===================================================

    /**
     * 지원하는 이벤트 타입인지 검증합니다.
     * 지원하지 않는 이벤트 타입이면 예외를 발생시킵니다.
     *
     * @param type 검증할 이벤트 타입
     * @throws IllegalArgumentException 지원하지 않는 이벤트 타입일 경우
     */
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
