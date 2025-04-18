package com.example.jobstat.core.event.outbox

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.global.utils.id_generator.SnowflakeGenerator
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class OutboxEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dataSerializer: DataSerializer,
    private val eventIdGenerator: SnowflakeGenerator,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.info("아웃박스 이벤트 생성: type=$type, payloadType=${payload::class.simpleName}")

        try {
            val eventId = eventIdGenerator.nextId()
            log.debug("이벤트 ID 생성: eventId=$eventId")
            log.debug("이벤트 타입 확인: eventType=$type")

            val event =
                Event
                    .of(
                        eventId = eventId,
                        type = type,
                        payload = payload,
                    ).toJson(dataSerializer)

            val outbox =
                Outbox.create(
                    eventType = type,
                    event = event,
                )

            log.info("아웃박스 이벤트 발행 요청: eventId=$eventId, type=$type")
            applicationEventPublisher.publishEvent(outbox)

            log.info("아웃박스 이벤트 발행 요청 완료: type=$type")
        } catch (e: Exception) {
            log.error("아웃박스 이벤트 발행 중 오류: type=$type, error=${e.message}", e)
            throw e
        }
    }
}
