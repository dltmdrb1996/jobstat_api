package com.example.jobstat.core.event.consumer

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment

/**
 * 제네릭 타입 기반 이벤트 컨슈머 추상 클래스.
 * 이벤트 처리 로직을 EventHandlerRegistry에 위임합니다.
 * 예외 발생 시 재처리를 위해 예외를 다시 던집니다. (@RetryableTopic 사용 가정)
 * 파싱 실패, 핸들러 미존재 등 특정 예외를 구분하여 로깅/처리합니다.
 */
abstract class AbstractEventConsumer {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Autowired
    protected lateinit var handlerRegistry: EventHandlerRegistry

    @Autowired
    protected lateinit var dataSerializer: DataSerializer

    open fun getSupportedEventTypes(): Set<EventType> = handlerRegistry.getSupportedEventTypes()

    protected fun consumeEvent(
        jsonEvent: String,
        ack: Acknowledgment,
    ) {
        lateinit var event: Event<*>

        try {
            try {
                event = Event.fromJson(jsonEvent, dataSerializer)
                log.debug("[{}] 이벤트 파싱 성공: eventId=${event.eventId}, type=${event.type}", this::class.simpleName)
            } catch (e: Exception) {
                log.error("[${this::class.simpleName}] 이벤트 파싱 중 예외 발생 (기타): rawJson=${jsonEvent.take(500)}, error=${e.message}. 메시지 재시도/DLT 처리 예정.", e)
                throw e
            }

            val eventType = event.type
            val eventId = event.eventId

            log.info("[${this::class.simpleName}] 이벤트 수신 및 파싱 완료: eventId=$eventId, type=$eventType, topic=${eventType.topic}")

            try {
                log.info("[${this::class.simpleName}}] 이벤트 처리 시작: eventId=$eventId, type=$eventType, payloadType=${event.payload::class.simpleName}")
                handlerRegistry.processEvent(event)
                log.info("[${this::class.simpleName}] 이벤트 처리 성공: eventId=$eventId, type=$eventType.")
            } catch (e: Exception) {
                log.error("[${this::class.simpleName}] 이벤트 처리 중 오류 발생 (재시도/DLT 처리 예정): eventId=$eventId, type=$eventType, error=${e.message}")
                log.debug("[${this::class.simpleName}] eventId=$eventId 예외 스택 트레이스: ", e)
                throw e
            }

            ack.acknowledge()
            log.info("[${this::class.simpleName}] Kafka 메시지 수동 Acknowledge 완료: eventId=$eventId, type=$eventType")
        } catch (e: Exception) {
            log.warn("[${this::class.simpleName}] consumeEvent 처리 중 예외 발생하여 Ack 미수행 (재시도/DLT 처리 예정). Error: {}", e.message)
            log.error("[${this::class.simpleName}] 최종 예외를 Kafka 리스너 컨테이너로 전파합니다.", e)
            throw e
        }
    }
}
