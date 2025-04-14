// file: src/main/kotlin/com/example/jobstat/core/event/outbox/OutboxProcessor.kt
package com.example.jobstat.core.event.outbox

import com.example.jobstat.core.event.dlt.CustomDltHeaders
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class OutboxProcessor(
    private val outboxRepository: OutboxRepository,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${outbox.processor.kafka-send-timeout-seconds:5}")
    private val kafkaSendTimeoutSeconds: Long,
    @Value("\${outbox.processor.max-retry-count:3}")
    private val maxRetryCount: Int,
    @Value("\${kafka.consumer.common.dlt-suffix:.DLT}")
    private val dltSuffix: String,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        private const val OUTBOX_FAILURE_SOURCE = "OUTBOX_PUBLISHER"
        private const val DEFAULT_EVENT_ID_PREFIX = "unknown-outbox-"
        private const val UNKNOWN_EVENT_TYPE = "unknown"
    }

    @Transactional
    fun processOutboxItem(outbox: Outbox) {
        if (outbox.retryCount >= maxRetryCount) {
            log.warn(
                "이미 최대 재시도 횟수({})에 도달한 Outbox 항목(ID: {}) 처리를 건너<0xEB><0x8E>>니다.",
                maxRetryCount, outbox.id
            )
            return
        }

        log.info("아웃박스 처리 시작 (스케줄러): id=${outbox.id}, type=${outbox.eventType}, retryCount=${outbox.retryCount}")
        retryEventExecute(outbox)
    }

    private fun retryEventExecute(outbox: Outbox) {
        try {
            log.debug("Kafka 메시지 재전송 시도: id=${outbox.id}, topic=${outbox.eventType.topic}")

            val sendResult = outboxKafkaTemplate.send(
                outbox.eventType.topic,
                outbox.id.toString(),
                outbox.event,
            ).get(kafkaSendTimeoutSeconds, TimeUnit.SECONDS)

            val recordMetadata = sendResult.recordMetadata
            log.debug("Kafka 재전송 응답 수신: id=${outbox.id}, partition=${recordMetadata?.partition()}, offset=${recordMetadata?.offset()}")

            outboxRepository.deleteById(outbox.id)
            log.info(
                "이벤트 재시도 성공 및 Outbox 삭제: id=${outbox.id}, type=${outbox.eventType}, retryCount=${outbox.retryCount}",
            )
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is TimeoutException -> "Kafka 전송 확인 대기 중 타임아웃 ($kafkaSendTimeoutSeconds 초)"
                else -> e.message ?: "알 수 없는 Kafka 전송 오류"
            }
            log.error(
                "이벤트 재시도 실패: id=${outbox.id}, type=${outbox.eventType}, retryCount=${outbox.retryCount}, error='$errorMessage'",
                e
            )
            processFailedAttempt(outbox, errorMessage)
        }
    }

    private fun processFailedAttempt(outbox: Outbox, errorMessage: String) {
        val nextRetryCount = outbox.retryCount + 1

        if (nextRetryCount >= maxRetryCount) {
            log.warn("재시도 실패 후 최대 횟수(${maxRetryCount}) 도달: id=${outbox.id}. DLT 전송 시도 및 Outbox 상태 업데이트.",)
            attemptDltSendAndUpdateStatus(outbox, nextRetryCount, errorMessage)
        } else {
            try {
                outbox.incrementRetryCount()
                outboxRepository.save(outbox)
                log.info(
                    "재시도 횟수 증가 (Dirty Check): id=${outbox.id}, type=${outbox.eventType}, newRetryCount=${outbox.retryCount}",
                )
            } catch (e: Exception) {
                log.error("Outbox 재시도 횟수 업데이트 중 예상치 못한 오류: id=${outbox.id}, error=${e.message}", e)
                throw e
            }
        }
    }

    /**
     * DLT 전송을 시도하고, 결과에 따라 Outbox 레코드를 삭제하거나 재시도 횟수만 업데이트합니다.
     */
    private fun attemptDltSendAndUpdateStatus(outbox: Outbox, finalRetryCount: Int, errorMessage: String) {
        var dltSendSuccessful = false
        try {
            // 1. DLT 메시지 준비
            val originalPayload = outbox.event
            val (eventId, eventType) = extractEventMetadata(originalPayload, outbox.id.toString(), outbox.eventType.name)
            val dltTopic = outbox.eventType.topic + dltSuffix

            // 2. 커스텀 헤더 생성
            val headers = listOf<Header>(
                RecordHeader(CustomDltHeaders.X_FAILURE_SOURCE, OUTBOX_FAILURE_SOURCE.toByteArray(StandardCharsets.UTF_8)),
                RecordHeader(CustomDltHeaders.X_RETRY_COUNT, finalRetryCount.toString().toByteArray(StandardCharsets.UTF_8)),
                RecordHeader(CustomDltHeaders.X_LAST_ERROR, errorMessage.take(500).toByteArray(StandardCharsets.UTF_8))
            )

            // 3. ProducerRecord 생성
            val producerRecord = ProducerRecord<String, String>(
                dltTopic, null, eventId, originalPayload, headers
            )

            // 4. DLT로 메시지 전송 (동기 방식)
            log.info("Outbox 실패 메시지 DLT 전송 시도: id=${outbox.id}, dltTopic=$dltTopic, eventId=$eventId")
            val sendResult = outboxKafkaTemplate.send(producerRecord).get(kafkaSendTimeoutSeconds, TimeUnit.SECONDS)
            log.info(
                "Outbox 실패 메시지 DLT 전송 성공: id=${outbox.id}, dltTopic=$dltTopic, offset=${sendResult.recordMetadata?.offset()}",
            )
            dltSendSuccessful = true // DLT 전송 성공 플래그 설정

        } catch (e: Exception) {
            // DLT 전송 자체도 실패하는 경우
            log.error(
                "치명적 오류: Outbox 실패 메시지 DLT 전송 실패! Outbox 레코드는 유지됩니다(retryCount={}). 수동 확인 필요. " +
                        "outboxId=${outbox.id}, type=${outbox.eventType}, dltTopic=${outbox.eventType.topic + dltSuffix}, error=${e.message}",
                finalRetryCount, e
            )
            // dltSendSuccessful 플래그는 false로 유지됨
        }

        // 5. DLT 전송 결과에 따라 Outbox 처리
        try {
            if (dltSendSuccessful) {
                // DLT 전송 성공 시 Outbox 레코드 삭제
                outboxRepository.deleteById(outbox.id)
                log.info("DLT 전송 성공 후 Outbox 레코드 삭제 완료: id=${outbox.id}")
            } else {
                // DLT 전송 실패 시 Outbox 레코드 유지하되, retryCount는 최종 값으로 업데이트하여 더 이상 처리되지 않도록 함
                if (outbox.retryCount < finalRetryCount) { // 이미 업데이트된 경우는 제외
                    outbox.retryCount = finalRetryCount
                    log.warn("DLT 전송 실패로 Outbox 레코드 유지 및 retryCount 업데이트: id={}, newRetryCount={}", outbox.id, outbox.retryCount)
                    // save() 는 트랜잭션 커밋 시 자동으로 호출됨 (Dirty Checking)
                } else {
                    log.warn("DLT 전송 실패. Outbox 레코드(ID: {})는 이미 최종 재시도 횟수({}) 상태이므로 유지됩니다.", outbox.id, outbox.retryCount)
                }
            }
        } catch (updateOrDeleteEx: Exception) {
            log.error(
                "Outbox 레코드 최종 처리(삭제 또는 업데이트) 중 오류 발생: id={}, dltSentSuccess={}, error={}",
                outbox.id, dltSendSuccessful, updateOrDeleteEx.message, updateOrDeleteEx
            )
            // 이 경우에도 예외를 던져 트랜잭션 롤백 유도 (DB 상태 불일치 방지)
            throw updateOrDeleteEx
        }
    }

    private fun extractEventMetadata(payload: String, fallbackEventId: String, fallbackEventType: String): Pair<String, String> {
        var eventId = fallbackEventId
        var eventType = fallbackEventType
        try {
            val jsonNode = objectMapper.readTree(payload)
            eventId = jsonNode.path("eventId").asText(fallbackEventId)
            eventType = jsonNode.path("type").asText(fallbackEventType)
            if (eventId.isBlank()) eventId = fallbackEventId
            if (eventType.isBlank()) eventType = fallbackEventType
        } catch (e: Exception) {
            log.warn("Outbox 페이로드에서 eventId/type 파싱 실패 (Fallback 사용): fallbackEventId=$fallbackEventId, error=${e.message}")
        }
        return Pair(eventId, eventType)
    }
}