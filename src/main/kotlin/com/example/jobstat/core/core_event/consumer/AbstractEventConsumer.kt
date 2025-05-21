package com.example.jobstat.core.core_event.consumer // 패키지 변경

import com.example.jobstat.core.core_event.model.Event
import com.example.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment

abstract class AbstractEventConsumer {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Autowired
    protected lateinit var handlerRegistry: EventHandlerRegistry

    @Autowired
    protected lateinit var dataSerializer: DataSerializer

    protected fun consumeEvent(
        jsonEvent: String,
        ack: Acknowledgment,
        consumerIdentifier: String // 로깅을 위한 컨슈머 식별자 추가
    ) {
        lateinit var event: Event<*> // Event<out EventPayload> 로 변경 가능

        try {
            try {
                // Event.fromJson은 Event<EventPayload>를 반환.
                event = Event.fromJson(jsonEvent, dataSerializer)
                log.debug("[{}] 이벤트 파싱 성공: eventId=${event.eventId}, type=${event.type}", consumerIdentifier)
            } catch (e: Exception) { // 구체적인 파싱 예외를 잡는 것이 좋음 (예: JsonProcessingException)
                log.error(
                    "[{}] 이벤트 파싱 중 예외 발생: rawJson={}, error={}. 메시지 재시도/DLT 처리 예정.",
                    consumerIdentifier, jsonEvent.take(500), e.message, e
                )
                throw e // 파싱 실패 시 재시도/DLT로 넘김
            }

            val eventType = event.type
            val eventId = event.eventId

            log.debug(
                "[{}] 이벤트 수신 및 파싱 완료: eventId={}, type={}, topic={}, payloadType={}",
                consumerIdentifier, eventId, eventType, eventType.topic, event.payload::class.simpleName
            )

            try {
                log.debug("[{}] 이벤트 처리 시작: eventId={}, type={}", consumerIdentifier, eventId, eventType)
                // EventHandlerRegistry의 processEvent가 internal이므로 직접 호출
                handlerRegistry.processEvent(event) // event의 타입은 Event<out EventPayload>
                log.debug("[{}] 이벤트 처리 성공: eventId={}, type={}.", consumerIdentifier, eventId, eventType)

            } catch (e: Exception) { // 핸들러 실행 중 발생한 모든 예외
                log.error(
                    "[{}] 이벤트 처리 중 오류 발생 (재시도/DLT 처리 예정): eventId={}, type={}, error={}",
                    consumerIdentifier, eventId, eventType, e.message
                )
                // 상세 스택 트레이스는 DEBUG 레벨에서만 로깅하거나 Sentry 등으로 전송
                log.debug("[{}] eventId={} 예외 스택 트레이스: ", consumerIdentifier, eventId, e)
                throw e // 예외를 다시 던져 Kafka 리스너가 재시도/DLT 처리하도록 함
            }

            ack.acknowledge()
            log.debug(
                "[{}] Kafka 메시지 수동 Acknowledge 완료: eventId={}, type={}",
                consumerIdentifier, eventId, eventType
            )

        } catch (e: Exception) { // consumeEvent 메소드 내에서 발생한 모든 최종 예외 처리
            // 이 블록에 도달했다는 것은 ack.acknowledge()가 호출되지 않았음을 의미
            log.warn(
                "[{}] consumeEvent 처리 중 최종 예외 발생하여 Ack 미수행 (재시도/DLT 처리 예정). Error: {}",
                consumerIdentifier, e.message
            )
            // Kafka 리스너 컨테이너로 예외를 전파하여 RetryableTopic 등이 동작하도록 함
            // 여기서 throw e를 하지 않으면 메시지가 ack된 것으로 간주될 수 있음(에러 핸들러 설정에 따라 다름)
            throw e
        }
    }
}