package com.example.jobstat.core.event.dlt

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders // Spring Kafka 제공 표준 헤더 이름 상수
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * 데드 레터 토픽 컨슈머
 * DLT로 전송된 실패 메시지를 수신하여 처리합니다.
 * Outbox 발행 실패와 구독/핸들링 실패를 구분하여 DB에 저장합니다.
 */
@Component
class DLTConsumer(
    private val deadLetterTopicRepository: DeadLetterTopicRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val SUBSCRIBER_FAILURE_SOURCE = "KAFKA_CONSUMER"
        private const val UNKNOWN_EVENT_TYPE = "unknown"
        private const val DEFAULT_EVENT_ID_PREFIX = "unknown-dlt-"
    }

    @KafkaListener(
        topicPattern = ".*\\.DLT",
        groupId = "\${dlt.consumer.group-id:dlt-persistence-group}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun processDltMessage(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val headers = record.headers()
        val originalPayload = record.value()
        val kafkaKey = record.key()
        val originalTopic = getHeaderAsString(headers, KafkaHeaders.ORIGINAL_TOPIC) ?: getHeaderAsString(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC) ?: "unknown" // Spring Kafka 버전에 따라 헤더 이름 다를 수 있음

        log.debug("DLT 메시지 수신: originalTopic=$originalTopic, key=$kafkaKey, partition=${record.partition()}, offset=${record.offset()}")

        try {
            val failureSource = getHeaderAsString(headers, CustomDltHeaders.X_FAILURE_SOURCE)
            val isOutboxFailure = failureSource != null

            log.debug(
                "DLT 메시지 출처: ${if (isOutboxFailure) "Outbox 발행 실패" else "구독/핸들링 실패"} (Header: ${CustomDltHeaders.X_FAILURE_SOURCE}=$failureSource)",
            )

            val retryCount: Int
            val lastError: String
            val eventId: String
            val eventType: String

            val (parsedEventId, parsedEventType) = extractEventMetadata(originalPayload, kafkaKey ?: DEFAULT_EVENT_ID_PREFIX + System.currentTimeMillis())

            if (isOutboxFailure) {
                log.debug("DLT 메시지 출처: Outbox 발행 실패 (Header: ${CustomDltHeaders.X_FAILURE_SOURCE}=$failureSource)")
                retryCount = getHeaderAsString(headers, CustomDltHeaders.X_RETRY_COUNT)?.toIntOrNull() ?: -1
                lastError = getHeaderAsString(headers, CustomDltHeaders.X_LAST_ERROR) ?: "N/A"
                eventId = parsedEventId
                eventType = parsedEventType
            } else {
                val exceptionMessage = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_MESSAGE) ?: "N/A"
                val stacktraceStr = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_STACKTRACE)
                val rootCauseFqcn =
                    getHeaderAsString(headers, KafkaHeaders.EXCEPTION_CAUSE_FQCN)
                        ?: getHeaderAsString(headers, KafkaHeaders.EXCEPTION_FQCN)

                var essentialInfo = "오류: $exceptionMessage"

                if (rootCauseFqcn != null) {
                    essentialInfo += "\n원인: $rootCauseFqcn"
                }

                if (stacktraceStr != null) {
                    val appPackagePrefix = "com.example."
                    val firstAppStackLine = stacktraceStr.lines().find { it.trim().startsWith("at $appPackagePrefix") }

                    if (firstAppStackLine != null) {
                        essentialInfo += "\n위치: ${firstAppStackLine.trim()}"
                    } else {
                        val firstAtLine = stacktraceStr.lines().find { it.trim().startsWith("at ") }
                        if (firstAtLine != null) {
                            essentialInfo += "\n위치(추정): ${firstAtLine.trim()}"
                        }
                    }
                }

                lastError = essentialInfo.take(500)
                log.debug("추출된 오류 정보: $lastError")

                retryCount = -1
                eventId = parsedEventId
                eventType = parsedEventType
            }

            val dltEvent =
                createAndSaveDltEvent(
                    eventId = eventId,
                    eventType = eventType,
                    retryCount = retryCount,
                    failureSource = failureSource ?: SUBSCRIBER_FAILURE_SOURCE,
                    lastError = lastError,
                    payload = originalPayload,
                )

            log.debug(
                "DLT 메시지를 DB에 성공적으로 저장: originalTopic=$originalTopic, eventId=$eventId, failureSource=${dltEvent.failureSource}, dbId=${dltEvent.id}",
            )

            ack.acknowledge()
        } catch (e: Exception) {
            log.error(
                "치명적 오류: DLT 메시지를 DB에 저장 실패! 수동 조치 필요. originalTopic=$originalTopic, key=$kafkaKey, error=${e.message}",
                e,
            )
        }
    }

    private fun extractEventMetadata(
        payload: String,
        fallbackEventId: String,
    ): Pair<String, String> {
        var eventId = fallbackEventId
        var eventType = UNKNOWN_EVENT_TYPE

        try {
            val jsonNode = objectMapper.readTree(payload)
            eventId = jsonNode.path("eventId").asText(fallbackEventId)
            eventType = jsonNode.path("type").asText(UNKNOWN_EVENT_TYPE)
            if (eventId.isBlank()) eventId = fallbackEventId
            if (eventType.isBlank()) eventType = UNKNOWN_EVENT_TYPE
        } catch (e: Exception) {
            log.error("DLT 페이로드 JSON에서 eventId/eventType 파싱 실패 (Fallback 사용): ${e.message}")
        }

        return Pair(eventId, eventType)
    }

    private fun createAndSaveDltEvent(
        eventId: String,
        eventType: String,
        retryCount: Int,
        failureSource: String,
        lastError: String?,
        payload: String,
    ): DeadLetterTopicEvent {
        val dltEvent =
            DeadLetterTopicEvent.create(
                eventId = eventId,
                eventType = eventType,
                retryCount = retryCount,
                failureSource = failureSource,
                lastError = lastError,
                payload = payload,
            )
        return deadLetterTopicRepository.save(dltEvent)
    }

    private fun getHeaderAsString(
        headers: Headers,
        headerName: String,
    ): String? = headers.lastHeader(headerName)?.value()?.toString(StandardCharsets.UTF_8)
}
