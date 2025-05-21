package com.example.jobstat.core.core_event.outbox // 패키지 변경

import com.example.jobstat.core.core_event.model.Event
import com.example.jobstat.core.core_event.model.EventPayload
import com.example.jobstat.core.core_event.model.EventType
import com.example.jobstat.core.core_jpa_base.id_generator.SnowflakeGenerator
import com.example.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
// import org.springframework.stereotype.Component // Auto-config에서 Bean으로 등록

// @Component // 제거
class OutboxEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dataSerializer: DataSerializer,
    private val eventIdGenerator: SnowflakeGenerator
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    // 이 메소드는 외부에 노출되는 API
    fun publish(
        type: EventType,
        payload: EventPayload,
    ) {
        log.debug("아웃박스 이벤트 생성 요청: type={}, payloadType={}", type, payload::class.simpleName)
        try {
            val eventId = eventIdGenerator.nextId()
            log.debug("생성된 이벤트 ID: eventId={}", eventId)

            val eventJson =
                Event.of( // Event.of 사용
                    eventId = eventId,
                    type = type,
                    payload = payload,
                ).toJson(dataSerializer) // Event 클래스 내 toJson 사용

            val outbox =
                Outbox.create( // Outbox.create 사용
                    eventType = type,
                    event = eventJson,
                )

            log.debug("ApplicationEvent 발행 시도: outboxId(JPA ID, 아직 없음)={}, eventId={}, type={}", eventId, type)
            applicationEventPublisher.publishEvent(outbox) // Outbox 객체를 이벤트로 발행
            log.debug("ApplicationEvent 발행 완료: eventId={}, type={}", eventId, type)
        } catch (e: Exception) {
            log.error("아웃박스 이벤트 발행 중 오류 발생: type={}, error={}", type, e.message, e)
            throw e // 예외를 호출 측으로 전파
        }
    }
}