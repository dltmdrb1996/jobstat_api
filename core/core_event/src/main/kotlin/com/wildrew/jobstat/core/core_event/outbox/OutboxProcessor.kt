package com.wildrew.jobstat.core.core_event.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_event.dlt.CustomDltHeaders
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

open class OutboxProcessor(
    private val outboxRepository: OutboxRepository,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val kafkaSendTimeoutSeconds: Long,
    private val maxRetryCount: Int,
    private val dltSuffix: String,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        private const val OUTBOX_FAILURE_SOURCE = "OUTBOX_PUBLISHER"

        // private const val DEFAULT_EVENT_ID_PREFIX = "unknown-outbox-" // extractEventMetadata에서 사용
        private const val UNKNOWN_EVENT_TYPE = "unknown" // extractEventMetadata에서 사용
    }

    // 이 메소드는 스케줄러에 의해 호출되므로 public 이어야 함.
    // 각 Outbox 아이템 처리는 개별 트랜잭션으로 실행되어야 함.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun processOutboxItem(outbox: Outbox) {
        if (outbox.retryCount >= maxRetryCount) {
            log.warn(
                "최대 재시도 횟수({}) 도달, Outbox 항목(ID: {}) 처리 건너<0xEB><0x8E><0x84>. DLT 전송은 이전 시도에서 이루어졌어야 함.",
                maxRetryCount,
                outbox.id,
            )
            // 이 경우, attemptDltSendAndUpdateStatus에서 이미 DLT로 갔거나, 문제가 있어 Outbox에 남은 상태일 수 있음.
            // 추가적인 모니터링/알람 로직이 필요할 수 있음.
            return
        }

        log.debug("아웃박스 처리 시작 (스케줄러): id={}, type={}, retryCount={}", outbox.id, outbox.eventType, outbox.retryCount)
        try {
            retryEventExecuteInternal(outbox) // 내부 호출로 변경
        } catch (e: Exception) {
            // retryEventExecuteInternal 에서 예외를 처리하고, processFailedAttempt를 호출함.
            // processFailedAttempt 내부에서 DLT 전송 실패 등으로 심각한 오류 발생 시 여기에 도달할 수 있음.
            // 이 경우, 트랜잭션은 롤백됨 (REQUIRES_NEW). Outbox 레코드는 원래 상태로 유지.
            log.error(
                "Outbox 항목(ID: {}) 처리 중 심각한 오류 발생. 트랜잭션 롤백됨. Error: {}",
                outbox.id,
                e.message,
                e,
            )
            // 이 예외를 다시 던져서 스케줄러 호출부에서 인지하도록 할 수 있음 (선택적)
            // throw e;
        }
    }

    // @Transactional은 AOP 프록시를 통해 동작하므로, private 메소드에 직접 적용해도 효과 없음.
    // processOutboxItem의 트랜잭션 컨텍스트 내에서 실행됨.
    private fun retryEventExecuteInternal(outbox: Outbox) {
        try {
            log.debug("Kafka 메시지 발행 시도: id={}, topic={}", outbox.id, outbox.eventType.getTopicName())

            val sendResult =
                outboxKafkaTemplate
                    .send(outbox.eventType.getTopicName(), outbox.id.toString(), outbox.event)
                    .get(kafkaSendTimeoutSeconds, TimeUnit.SECONDS)

            val recordMetadata = sendResult.recordMetadata
            log.info( // 성공 시 INFO 레벨
                "Kafka 메시지 발행 성공: id={}, topic={}, partition={}, offset={}",
                outbox.id,
                outbox.eventType.getTopicName(),
                recordMetadata.partition(),
                recordMetadata.offset(),
            )

            outboxRepository.deleteById(outbox.id) // 현재 트랜잭션 내에서 삭제
            log.debug("Outbox 레코드 삭제 완료: id={}", outbox.id)
        } catch (e: Exception) {
            // Kafka 발행 실패 (Timeout, Interrupted, ExecutionException 등)
            val errorMessage =
                when (e) {
                    is TimeoutException -> "Kafka 전송 확인 대기 중 타임아웃 ($kafkaSendTimeoutSeconds 초)"
                    is InterruptedException -> {
                        Thread.currentThread().interrupt() // 인터럽트 상태 복원
                        "Kafka 전송 중 스레드 인터럽트 발생"
                    }
                    else -> e.message ?: "알 수 없는 Kafka 전송 오류"
                }
            log.error(
                "Kafka 메시지 발행 실패: id={}, type={}, retryCount={}, error='{}'",
                outbox.id,
                outbox.eventType,
                outbox.retryCount,
                errorMessage,
                e,
            )
            // 실패 처리 로직 호출 (트랜잭션은 계속 진행됨. 여기서 롤백되지 않음)
            processFailedAttempt(outbox, errorMessage)
            // 여기서 예외를 다시 던지면 processOutboxItem의 트랜잭션이 롤백됨.
            // 하지만 processFailedAttempt에서 outbox 상태를 변경하므로, 그대로 두는 것이 맞음.
            // 만약 processFailedAttempt에서 심각한 오류가 발생하면 그 예외가 전파되어 롤백될 것임.
        }
    }

    // 이 메소드도 processOutboxItem의 트랜잭션 컨텍스트 내에서 실행됨
    private fun processFailedAttempt(
        outbox: Outbox,
        errorMessage: String,
    ) {
        val currentRetryCount = outbox.retryCount // 변경 전 값
        val nextRetryCount = currentRetryCount + 1

        if (nextRetryCount >= maxRetryCount) {
            log.warn(
                "Kafka 발행 재시도 실패 후 최대 횟수({}) 도달: outboxId={}. DLT 전송 시도 및 Outbox 레코드 상태 업데이트.",
                maxRetryCount,
                outbox.id,
            )
            // DLT 전송 및 Outbox 레코드 상태 변경 (삭제 또는 retryCount 업데이트)
            // 이 작업은 현재 트랜잭션 내에서 수행됨
            attemptDltSendAndUpdateStatus(outbox, nextRetryCount, errorMessage)
        } else {
            // 재시도 횟수만 증가시키고 저장 (현재 트랜잭션 내에서)
            try {
                outbox.incrementRetryCount() // retryCount가 내부적으로 변경됨
                // outboxRepository.save(outbox) // JPA 경우 dirty checking으로 자동 업데이트될 수 있으나 명시적 save가 안전할 수 있음
                // 현재는 @Transactional 범위 내에 있으므로, 메소드 종료 시 outbox의 변경사항이 DB에 반영됨 (JPA Dirty Checking)
                log.debug(
                    "Outbox 재시도 횟수 증가: id={}, type={}, newRetryCount={}",
                    outbox.id,
                    outbox.eventType,
                    outbox.retryCount,
                )
            } catch (e: Exception) {
                // 이 예외는 processOutboxItem의 트랜잭션을 롤백시킴
                log.error("Outbox 재시도 횟수 업데이트 중 DB 오류 발생: id={}, error={}", outbox.id, e.message, e)
                throw RuntimeException("Outbox 재시도 횟수 업데이트 실패: ${e.message}", e) // 예외 전파
            }
        }
    }

    // 이 메소드도 processOutboxItem의 트랜잭션 컨텍스트 내에서 실행됨
    private fun attemptDltSendAndUpdateStatus(
        outbox: Outbox,
        finalRetryCount: Int, // 현재 시점에서의 최종 재시도 횟수 (maxRetryCount와 같을 것임)
        errorMessage: String,
    ) {
        var dltSendSuccessful = false
        val dltTopic = outbox.eventType.getTopicName() + dltSuffix
        try {
            val originalPayload = outbox.event
            // fallbackEventId는 outbox.id (Long)를 String으로 변환
            val (eventId, _) = extractEventMetadata(originalPayload, outbox.id.toString(), outbox.eventType.name)

            val headers =
                listOf<Header>(
                    RecordHeader(CustomDltHeaders.X_FAILURE_SOURCE, OUTBOX_FAILURE_SOURCE.toByteArray(StandardCharsets.UTF_8)),
                    RecordHeader(CustomDltHeaders.X_RETRY_COUNT, finalRetryCount.toString().toByteArray(StandardCharsets.UTF_8)),
                    RecordHeader(CustomDltHeaders.X_LAST_ERROR, errorMessage.take(500).toByteArray(StandardCharsets.UTF_8)),
                )
            val producerRecord = ProducerRecord<String, String>(dltTopic, null, eventId, originalPayload, headers)

            log.debug("Outbox 실패 메시지 DLT 전송 시도: outboxId={}, dltTopic={}, eventId={}", outbox.id, dltTopic, eventId)
            val sendResult = outboxKafkaTemplate.send(producerRecord).get(kafkaSendTimeoutSeconds, TimeUnit.SECONDS)
            log.info( // DLT 전송 성공은 INFO 레벨
                "Outbox 실패 메시지 DLT 전송 성공: outboxId={}, dltTopic={}, kafkaOffset={}",
                outbox.id,
                dltTopic,
                sendResult.recordMetadata?.offset(),
            )
            dltSendSuccessful = true
        } catch (e: Exception) {
            // DLT 전송 실패는 심각한 문제로 간주. 트랜잭션 롤백 유도.
            log.error(
                "치명적 오류: Outbox 실패 메시지 DLT 전송 실패! Outbox 레코드(ID: {})는 현재 트랜잭션 롤백으로 유지됩니다 (retryCount={}). 수동 확인 필요. DltTopic={}, Error={}",
                outbox.id,
                outbox.retryCount,
                dltTopic,
                e.message,
                e,
            )
            // 여기서 예외를 던져서 processOutboxItem의 트랜잭션을 롤백시킴.
            // Outbox 레코드는 DB에 변경 없이 (또는 이전 상태로 롤백되어) 남게 됨.
            throw RuntimeException("DLT 전송 실패: ${e.message}", e)
        }

        // DLT 전송이 성공했을 경우에만 Outbox 레코드 삭제
        // DLT 전송 실패 시에는 위에서 예외가 던져져 이 코드에 도달하지 않음 (트랜잭션 롤백됨)
        if (dltSendSuccessful) {
            try {
                outboxRepository.deleteById(outbox.id) // 현재 트랜잭션 내에서 삭제
                log.debug("DLT 전송 성공 후 Outbox 레코드(ID: {}) 삭제 완료", outbox.id)
            } catch (e: Exception) {
                // DLT 전송은 성공했으나 Outbox 삭제 실패 시. 트랜잭션 롤백 유도.
                // 이 경우 DLT에는 메시지가 갔지만, Outbox에도 남아있게 되어 중복 처리 가능성. (DLT 컨슈머 멱등성 중요)
                log.error(
                    "DLT 전송은 성공했으나 Outbox 레코드(ID: {}) 삭제 중 DB 오류 발생. 트랜잭션 롤백됨. Error: {}",
                    outbox.id,
                    e.message,
                    e,
                )
                throw RuntimeException("Outbox 삭제 실패 (DLT는 성공): ${e.message}", e)
            }
        }
        // DLT 전송 실패 시에는 위에서 예외가 발생하여 트랜잭션이 롤백되므로,
        // outbox.retryCount = finalRetryCount 같은 코드는 실행되지 않음 (실행될 필요도 없음).
    }

    private fun extractEventMetadata(
        payload: String,
        fallbackEventId: String,
        fallbackEventType: String,
    ): Pair<String, String> {
        var eventId = fallbackEventId
        var eventType = fallbackEventType
        try {
            val jsonNode = objectMapper.readTree(payload)
            jsonNode
                .path("eventId")
                .asText(null)
                ?.takeIf { it.isNotBlank() }
                ?.let { eventId = it }
            jsonNode
                .path("type")
                .asText(null)
                ?.takeIf { it.isNotBlank() }
                ?.let { eventType = it }
        } catch (e: Exception) {
            log.warn(
                "Outbox 페이로드에서 eventId/type 파싱 실패 (Fallback 사용): fallbackEventId={}, error={}",
                fallbackEventId,
                e.message,
            )
        }
        return Pair(eventId, eventType)
    }
}
