// file: src/main/kotlin/com/example/jobstat/core/event/dlt/DLTConsumer.kt
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

    // ===================================================
    // Kafka 리스너 메소드
    // ===================================================

    @KafkaListener(
        topicPattern = ".*\\.DLT", // 모든 .DLT 토픽 구독
        groupId = "\${dlt.consumer.group-id:dlt-persistence-group}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun processDltMessage(record: ConsumerRecord<String, String>, ack: Acknowledgment) {

        val headers = record.headers()
        val originalPayload = record.value()
        val kafkaKey = record.key() // Outbox 실패 시 Event ID 또는 Outbox ID, 구독 실패 시 원본 Key
        val originalTopic = getHeaderAsString(headers, KafkaHeaders.ORIGINAL_TOPIC) ?: getHeaderAsString(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC) ?: "unknown" // Spring Kafka 버전에 따라 헤더 이름 다를 수 있음

        log.info(
            "DLT 메시지 수신: originalTopic=$originalTopic, key=$kafkaKey, partition=${record.partition()}, offset=${record.offset()}",
        )

        try {
            // 실패 정보 추출 (Outbox 실패 / 구독 실패 구분)
            val failureSource = getHeaderAsString(headers, CustomDltHeaders.X_FAILURE_SOURCE)
            val isOutboxFailure = failureSource != null

            log.info(
                "DLT 메시지 출처: ${if (isOutboxFailure) "Outbox 발행 실패" else "구독/핸들링 실패"} (Header: ${CustomDltHeaders.X_FAILURE_SOURCE}=$failureSource)",
            )

            val retryCount: Int
            val lastError: String
            val eventId: String
            val eventType: String

            // 이벤트 메타데이터 파싱 (공통) - 실패할 경우 대비하여 기본값 설정
            val (parsedEventId, parsedEventType) = extractEventMetadata(originalPayload, kafkaKey ?: DEFAULT_EVENT_ID_PREFIX + System.currentTimeMillis())

            if (isOutboxFailure) {
                // Outbox 발행 실패 케이스
                log.info("DLT 메시지 출처: Outbox 발행 실패 (Header: ${CustomDltHeaders.X_FAILURE_SOURCE}=$failureSource)")
                retryCount = getHeaderAsString(headers, CustomDltHeaders.X_RETRY_COUNT)?.toIntOrNull() ?: -1
                lastError = getHeaderAsString(headers, CustomDltHeaders.X_LAST_ERROR) ?: "N/A"
                eventId = parsedEventId
                eventType = parsedEventType

            } else {
                // 구독/핸들링 실패 케이스 (기존 로직)
                log.info("DLT 메시지 출처: 구독/핸들링 실패 (커스텀 헤더 없음)")
                val failureSourceConst = SUBSCRIBER_FAILURE_SOURCE

                val exceptionMessage = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_MESSAGE) ?: "N/A"
                val stacktraceStr = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_STACKTRACE)
                val rootCauseFqcn = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_CAUSE_FQCN) // 루트 원인 클래스명
                    ?: getHeaderAsString(headers, KafkaHeaders.EXCEPTION_FQCN) // 없으면 최상위 예외 클래스명 사용

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
                log.info("추출된 오류 정보: $lastError")

                retryCount = -1
                eventId = parsedEventId
                eventType = parsedEventType
            }

            // DLT 이벤트 엔티티 생성 및 저장
            val dltEvent = createAndSaveDltEvent(
                eventId = eventId,
                eventType = eventType,
                retryCount = retryCount,
                failureSource = failureSource ?: SUBSCRIBER_FAILURE_SOURCE, // Null 이면 구독 실패
                lastError = lastError,
                payload = originalPayload,
            )

            log.info(
                "DLT 메시지를 DB에 성공적으로 저장: originalTopic=$originalTopic, eventId=$eventId, failureSource=${dltEvent.failureSource}, dbId=${dltEvent.id}",
            )

            // 저장 성공 후 Kafka 메시지 확인응답
            ack.acknowledge()

        } catch (e: Exception) {
            log.error(
                "치명적 오류: DLT 메시지를 DB에 저장 실패! 수동 조치 필요. originalTopic=$originalTopic, key=$kafkaKey, error=${e.message}",
                e,
            )
            // Acknowledge 하지 않아 메시지가 다시 처리될 수 있음 (Broker 설정 및 Listener 동작 방식에 따라 다름)
        }
    }

    // ===================================================
    // 유틸리티 메소드
    // ===================================================

    /**
     * 이벤트 페이로드에서 메타데이터(이벤트 ID, 이벤트 타입)를 추출합니다.
     */
    private fun extractEventMetadata(payload: String, fallbackEventId: String): Pair<String, String> {
        var eventId = fallbackEventId
        var eventType = UNKNOWN_EVENT_TYPE

        try {
            val jsonNode = objectMapper.readTree(payload)
            // Event 클래스의 실제 필드명으로 path() 내부 문자열 수정 필요
            eventId = jsonNode.path("eventId").asText(fallbackEventId)
            eventType = jsonNode.path("type").asText(UNKNOWN_EVENT_TYPE)
            if (eventId.isBlank()) eventId = fallbackEventId
            if (eventType.isBlank()) eventType = UNKNOWN_EVENT_TYPE
        } catch (e: Exception) {
            log.error("DLT 페이로드 JSON에서 eventId/eventType 파싱 실패 (Fallback 사용): ${e.message}")
        }

        return Pair(eventId, eventType)
    }

    /**
     * DLT 이벤트를 생성하고 저장합니다.
     */
    private fun createAndSaveDltEvent(
        eventId: String,
        eventType: String,
        retryCount: Int,
        failureSource: String,
        lastError: String?, // Nullable 로 변경
        payload: String,
    ): DeadLetterTopicEvent {
        // DLT 이벤트 엔티티 생성
        val dltEvent = DeadLetterTopicEvent.create(
            eventId = eventId,
            eventType = eventType,
            retryCount = retryCount,
            failureSource = failureSource,
            lastError = lastError, // Null 허용
            payload = payload,
        )
        // 데이터베이스에 저장
        return deadLetterTopicRepository.save(dltEvent)
    }

    /**
     * Kafka 헤더에서 문자열 값을 추출합니다. (UTF-8 인코딩 가정)
     */
    private fun getHeaderAsString(headers: Headers, headerName: String): String? =
        headers.lastHeader(headerName)?.value()?.toString(StandardCharsets.UTF_8)
}