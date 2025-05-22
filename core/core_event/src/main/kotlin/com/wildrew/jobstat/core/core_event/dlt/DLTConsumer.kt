package com.wildrew.jobstat.core.core_event.dlt // 패키지 변경

import com.fasterxml.jackson.databind.ObjectMapper // core-serializer 또는 jackson 직접 의존
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener // Spring Kafka 의존
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.transaction.annotation.Transactional // 트랜잭션 필요 시
import java.nio.charset.StandardCharsets

// @Component // 제거
open class DLTConsumer(
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
        topicPattern = "#{'\${jobstat.core.event.dlt.consumer.topic-pattern:.*\\.DLT}'}", // SpEL
        groupId = "#{'\${jobstat.core.event.dlt.consumer.group-id:dlt-persistence-group}'}", // SpEL
        containerFactory = "coreKafkaListenerContainerFactory"
    )
    @Transactional
    open fun processDltMessage(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val headers = record.headers()
        val originalPayload = record.value()
        val kafkaKey = record.key()
        val originalTopic = getHeaderAsString(headers, KafkaHeaders.ORIGINAL_TOPIC)
            ?: getHeaderAsString(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC) ?: "unknown"

        log.debug("DLT 메시지 수신: originalTopic=$originalTopic, key=$kafkaKey, partition=${record.partition()}, offset=${record.offset()}")

        try {
            val failureSource = getHeaderAsString(headers, CustomDltHeaders.X_FAILURE_SOURCE)
            val isOutboxFailure = failureSource != null

            log.debug(
                "DLT 메시지 출처: {} (Header: {}={})",
                if (isOutboxFailure) "Outbox 발행 실패" else "구독/핸들링 실패",
                CustomDltHeaders.X_FAILURE_SOURCE, failureSource
            )

            val retryCount: Int
            val lastError: String
            val eventId: String
            val eventType: String

            val (parsedEventId, parsedEventType) = extractEventMetadata(
                originalPayload,
                kafkaKey ?: (DEFAULT_EVENT_ID_PREFIX + System.currentTimeMillis())
            )

            if (isOutboxFailure) {
                log.debug("DLT 메시지 출처: Outbox 발행 실패 (Header: {}={})", CustomDltHeaders.X_FAILURE_SOURCE, failureSource)
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
                rootCauseFqcn?.let { essentialInfo += "\n원인: $it" }

                stacktraceStr?.let {
                    val appPackagePrefix = "com.example."
                    val firstAppStackLine = it.lines().find { line -> line.trim().startsWith("at $appPackagePrefix") }
                    val stackLineToLog = firstAppStackLine
                        ?: it.lines().find { line -> line.trim().startsWith("at ") }
                    stackLineToLog?.let { line -> essentialInfo += "\n위치(추정): ${line.trim()}" }
                }
                lastError = essentialInfo.take(2000) // DB 컬럼 크기에 맞춤 (DeadLetterTopicEvent.lastError)
                log.debug("추출된 오류 정보: {}", lastError)

                retryCount = -1 // 구독 실패는 재시도 카운트 없음 (RetryableTopic에서 관리)
                eventId = parsedEventId
                eventType = parsedEventType
            }

            val dltEvent =
                DeadLetterTopicEvent.create( // DeadLetterTopicEvent.create 사용
                    eventId = eventId,
                    eventType = eventType,
                    retryCount = retryCount,
                    failureSource = failureSource ?: SUBSCRIBER_FAILURE_SOURCE,
                    lastError = lastError,
                    payload = originalPayload,
                )
            deadLetterTopicRepository.save(dltEvent) // 이 부분이 트랜잭션으로 보호됨

            log.info( // DLT 저장 성공은 INFO 레벨로
                "DLT 메시지를 DB에 성공적으로 저장: originalTopic={}, eventId={}, failureSource={}, dbId={}",
                originalTopic, eventId, dltEvent.failureSource, dltEvent.id
            )

            ack.acknowledge()
        } catch (e: Exception) { // DeadLetterTopicRepository.save() 실패 등
            log.error(
                "치명적 오류: DLT 메시지를 DB에 저장 실패! 수동 조치 필요. originalTopic={}, key={}, error={}",
                originalTopic, kafkaKey, e.message, e
            )
            // ack.acknowledge() 하지 않아서 메시지가 DLT에 다시 돌아오거나, nack 처리될 수 있음 (리스너 설정에 따라)
            // 또는, 여기서 RuntimeException을 던져서 컨테이너가 처리하도록 할 수 있음
            throw RuntimeException("DLT 메시지 처리 중 DB 저장 실패 (재시도 필요): ${e.message}", e)
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
            jsonNode.path("eventId").asText(null)?.takeIf { it.isNotBlank() }?.let { eventId = it }
            jsonNode.path("type").asText(null)?.takeIf { it.isNotBlank() }?.let { eventType = it }
        } catch (e: Exception) {
            log.warn("DLT 페이로드 JSON에서 eventId/eventType 파싱 실패 (Fallback 사용): {}", e.message)
        }
        return Pair(eventId, eventType)
    }

    private fun getHeaderAsString(
        headers: Headers,
        headerName: String,
    ): String? = headers.lastHeader(headerName)?.value()?.toString(StandardCharsets.UTF_8)
}