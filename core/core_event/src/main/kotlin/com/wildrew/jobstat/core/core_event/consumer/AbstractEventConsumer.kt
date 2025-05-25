package com.wildrew.jobstat.core.core_event.consumer

import com.wildrew.jobstat.core.core_event.model.Event
import com.wildrew.jobstat.core.core_serializer.DataSerializer
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
        consumerIdentifier: String,
    ) {
        lateinit var event: Event<*>

        try {
            try {
                event = Event.fromJson(jsonEvent, dataSerializer)
                log.debug("[{}] 이벤트 파싱 성공: eventId=${event.eventId}, type=${event.type}", consumerIdentifier)
            } catch (e: Exception) {
                log.error(
                    "[{}] 이벤트 파싱 중 예외 발생: rawJson={}, error={}. 메시지 재시도/DLT 처리 예정.",
                    consumerIdentifier,
                    jsonEvent.take(500),
                    e.message,
                    e,
                )
                throw e
            }

            val eventType = event.type
            val eventId = event.eventId

            log.debug(
                "[{}] 이벤트 수신 및 파싱 완료: eventId={}, type={}, topic={}, payloadType={}",
                consumerIdentifier,
                eventId,
                eventType,
                eventType.getTopicName(),
                event.payload::class.simpleName,
            )

            try {
                log.debug("[{}] 이벤트 처리 시작: eventId={}, type={}", consumerIdentifier, eventId, eventType)
                handlerRegistry.processEvent(event)
                log.debug("[{}] 이벤트 처리 성공: eventId={}, type={}.", consumerIdentifier, eventId, eventType)
            } catch (e: Exception) {
                log.error(
                    "[{}] 이벤트 처리 중 오류 발생 (재시도/DLT 처리 예정): eventId={}, type={}, error={}",
                    consumerIdentifier,
                    eventId,
                    eventType,
                    e.message,
                )
                log.debug("[{}] eventId={} 예외 스택 트레이스: ", consumerIdentifier, eventId, e)
                throw e
            }

            ack.acknowledge()
            log.debug(
                "[{}] Kafka 메시지 수동 Acknowledge 완료: eventId={}, type={}",
                consumerIdentifier,
                eventId,
                eventType,
            )
        } catch (e: Exception) {
            log.warn(
                "[{}] consumeEvent 처리 중 최종 예외 발생하여 Ack 미수행 (재시도/DLT 처리 예정). Error: {}",
                consumerIdentifier,
                e.message,
            )
            throw e
        }
    }
}
