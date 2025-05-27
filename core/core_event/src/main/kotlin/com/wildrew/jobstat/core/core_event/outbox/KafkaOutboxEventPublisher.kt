package com.wildrew.jobstat.core.core_event.outbox

import com.wildrew.jobstat.core.core_event.model.Event
import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_jpa_base.id_generator.SnowflakeGenerator
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher

class KafkaOutboxEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dataSerializer: DataSerializer,
    private val eventIdGenerator: SnowflakeGenerator,
) : OutboxEventPublisher {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.debug("아웃박스 이벤트 생성 요청: type={}, payloadType={}", type, payload::class.simpleName)
        try {
            val eventId = eventIdGenerator.nextId()
            log.debug("생성된 이벤트 ID: eventId={}", eventId)

            val eventJson =
                Event
                    .of(
                        eventId = eventId,
                        type = type,
                        payload = payload,
                    ).toJson(dataSerializer)

            val outbox =
                Outbox.create(
                    eventType = type,
                    event = eventJson,
                )

            log.debug("ApplicationEvent 발행 시도: outboxId(JPA ID, 아직 없음)={}, eventId={}, type={}", eventId, type)
            applicationEventPublisher.publishEvent(outbox)
            log.debug("ApplicationEvent 발행 완료: eventId={}, type={}", eventId, type)
        } catch (e: Exception) {
            log.error("아웃박스 이벤트 발행 중 오류 발생: type={}, error={}", type, e.message, e)
            throw e
        }
    }
}
