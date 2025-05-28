package com.wildrew.jobstat.core.core_event.consumer

import com.wildrew.jobstat.core.core_event.model.Event
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.time.Duration

abstract class AbstractEventConsumer {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Autowired
    protected lateinit var handlerRegistry: EventHandlerRegistryService

    @Autowired
    protected lateinit var dataSerializer: DataSerializer

    @Autowired
    protected lateinit var idempotencyChecker: IdempotencyChecker

    protected open val idempotencyKeyTtl: Duration = Duration.ofHours(1)

    protected fun consumeEvent(
        jsonEvent: String,
        ack: Acknowledgment,
        consumerIdentifier: String,
    ) {
        lateinit var event: Event<*>
        var idempotencyKey: Long? = null

        try {
            try {
                event = Event.fromJson(jsonEvent, dataSerializer)
                idempotencyKey = event.eventId
                log.debug("[{}] 이벤트 파싱 성공: eventId={}, type={}, IdempotencyKey={}", consumerIdentifier, event.eventId, event.type, idempotencyKey)
            } catch (e: Exception) {
                log.error(
                    "[{}] 이벤트 파싱 중 예외 발생: rawJson={}, error={}. 메시지 재시도/DLT 처리 예정.",
                    consumerIdentifier,
                    jsonEvent.take(500),
                    e.message,
                    e,
                )
                throw e // 파싱 실패 시 재시도 유도
            }

            if (idempotencyKey != null) {
                if (idempotencyChecker.isAlreadyProcessed(idempotencyKey)) {
                    log.warn(
                        "[{}] 이미 처리된 이벤트 수신 (멱등성 체크): IdempotencyKey={}. Ack 후 건너<0xEB><0x8E><0x84>.",
                        consumerIdentifier,
                        idempotencyKey,
                    )
                    ack.acknowledge() // 이미 처리되었으므로 ack 하고 종료
                    return
                }
            } else {
                log.warn("[{}] IdempotencyKey가 설정되지 않아 멱등성 체크를 건너<0xEB><0x8E><0x84>니다. eventId={}", consumerIdentifier, event.eventId)
                // IdempotencyKey가 없는 경우 어떻게 처리할지 정책 필요 (오류로 간주하거나, 경고 후 진행)
            }

            val eventType = event.type
            // eventId는 로그용으로 계속 사용 가능
            val originalEventIdForLog = event.eventId

            log.debug(
                "[{}] 이벤트 수신 및 파싱 완료 (처리 전): IdempotencyKey={}, eventId={}, type={}, topic={}, payloadType={}",
                consumerIdentifier,
                idempotencyKey,
                originalEventIdForLog,
                eventType,
                eventType.getTopicName(),
                event.payload::class.simpleName,
            )

            try {
                log.debug("[{}] 이벤트 처리 시작: IdempotencyKey={}, eventId={}", consumerIdentifier, idempotencyKey, originalEventIdForLog)
                // 2. 실제 이벤트 처리
                handlerRegistry.processEvent(event)
                log.debug("[{}] 이벤트 처리 성공: IdempotencyKey={}, eventId={}", consumerIdentifier, idempotencyKey, originalEventIdForLog)

                // 3. 처리 완료 마킹 (IdempotencyKey가 설정된 경우에만)
                // 이 작업은 이벤트 처리가 완전히 성공한 후에 수행되어야 함.
                // 만약 handlerRegistry.processEvent(event) 내부에서 예외가 발생하면 이 코드는 실행되지 않음.
                try {
                    idempotencyChecker.markAsProcessed(idempotencyKey, idempotencyKeyTtl)
                    log.debug("[{}] 이벤트 처리 완료 마킹 성공: IdempotencyKey={}", consumerIdentifier, idempotencyKey)
                } catch (e: Exception) {
                    // 처리 완료 마킹 실패 시 심각한 문제.
                    // 이벤트 처리는 성공했지만, 마킹 실패로 인해 다음번 동일 메시지 수신 시 중복 처리될 수 있음.
                    // 이 경우, ack를 하지 않고 재시도하도록 유도하거나 (메시지 전체 재처리)
                    // 또는 이 오류를 치명적으로 간주하고 시스템 관리자에게 알림을 보내야 함.
                    // 여기서는 예외를 다시 던져서 Kafka가 재시도하도록 함 (메시지 전체 재처리)
                    log.error(
                        "[{}] 이벤트 처리 완료 마킹 중 오류 발생 (재시도 예정): IdempotencyKey={}, error={}",
                        consumerIdentifier,
                        idempotencyKey,
                        e.message,
                        e,
                    )
                    throw e // 재시도 유도
                }
            } catch (e: Exception) {
                // handlerRegistry.processEvent(event) 또는 idempotencyChecker.markAsProcessed() 에서 발생한 예외
                log.error(
                    "[{}] 이벤트 처리 또는 완료 마킹 중 오류 발생 (재시도/DLT 처리 예정): IdempotencyKey={}, eventId={}, type={}, error={}",
                    consumerIdentifier,
                    idempotencyKey,
                    originalEventIdForLog,
                    eventType,
                    e.message,
                )
                log.debug("[{}] IdempotencyKey={} 예외 스택 트레이스: ", consumerIdentifier, idempotencyKey, e)
                throw e // 재시도 유도
            }

            // 4. Kafka 메시지 Acknowledge
            ack.acknowledge()
            log.debug(
                "[{}] Kafka 메시지 수동 Acknowledge 완료: IdempotencyKey={}, eventId={}",
                consumerIdentifier,
                idempotencyKey,
                originalEventIdForLog,
            )
        } catch (e: Exception) {
            log.warn(
                "[{}] consumeEvent 처리 중 최종 예외 발생하여 Ack 미수행 (재시도/DLT 처리 예정). IdempotencyKey={}, Error: {}",
                consumerIdentifier,
                idempotencyKey ?: "N/A",
                e.message,
            )
            throw e
        }
    }
}
