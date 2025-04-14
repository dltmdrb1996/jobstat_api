package com.example.jobstat.core.event.outbox

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.global.utils.id_generator.SnowflakeGenerator
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 아웃박스 이벤트 발행기
 * 이벤트를 생성하고 Spring Event로 발행하여 트랜잭션 내에서 안전하게 처리할 수 있게 합니다.
 */
@Component
class OutboxEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dataSerializer: DataSerializer,
    private val eventIdGenerator: SnowflakeGenerator,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 이벤트를 생성하고 발행합니다.
     *
     * @param type 이벤트 타입
     * @param payload 이벤트 페이로드
     */
    fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.info("아웃박스 이벤트 생성: type=$type, payloadType=${payload::class.simpleName}")

        try {
            // 1. 이벤트 ID 생성
            val eventId = eventIdGenerator.nextId()
            log.debug("이벤트 ID 생성: eventId=$eventId")
            log.debug("이벤트 타입 확인: eventType=$type")

            // 2. 이벤트 객체 생성 및 직렬화
            val event =
                Event
                    .of(
                        eventId = eventId,
                        type = type,
                        payload = payload,
                    ).toJson(dataSerializer)

            // 3. 아웃박스 객체 생성
            val outbox =
                Outbox.create(
                    eventType = type,
                    event = event,
                )

            // 4. 스프링 이벤트로 발행 (OutboxMessageRelay에서 처리)
            log.info("아웃박스 이벤트 발행 요청: eventId=$eventId, type=$type")
            applicationEventPublisher.publishEvent(outbox)

            log.info("아웃박스 이벤트 발행 요청 완료: type=$type")
        } catch (e: Exception) {
            log.error("아웃박스 이벤트 발행 중 오류: type=$type, error=${e.message}", e)
            throw e
        }
    }
}
