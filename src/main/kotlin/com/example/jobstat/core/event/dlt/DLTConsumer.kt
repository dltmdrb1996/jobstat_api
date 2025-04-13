package com.example.jobstat.core.event.dlt

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Component
class DLTConsumer(
    private val deadLetterTopicRepository: DeadLetterTopicRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    // groupId는 환경별로 다르게 설정하는 것이 좋을 수 있음
    @KafkaListener(
        topicPattern = ".*\\.DLT", // 모든 .DLT 토픽 구독
        groupId = "\${dlt.consumer.group-id:dlt-persistence-group}", // 설정 파일에서 그룹 ID 주입
        containerFactory = "kafkaListenerContainerFactory" // 설정된 컨테이너 팩토리 사용
    )
    fun processDltMessage(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val headers = record.headers()
        val originalTopic = getHeaderAsString(headers, KafkaHeaders.ORIGINAL_TOPIC) ?: "unknown"
        val exceptionMessage = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_MESSAGE) ?: "N/A"
        // 스택 트레이스는 매우 길 수 있으므로 필요 시에만 로깅하거나 길이를 제한
        val exceptionStacktrace = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_STACKTRACE)?.take(1500) ?: "N/A"
        val originalPayload = record.value() // 원본 이벤트 페이로드

        log.warn(
            "DLT 메시지 수신: originalTopic=${originalTopic}, key=${record.key()}, partition=${record.partition()}, offset=${record.offset()}, error='${exceptionMessage}'"
        )
        log.debug("DLT 예외 스택 트레이스: ${exceptionStacktrace}") // 필요시 DEBUG 레벨로 스택트레이스 확인

        try {
            // 페이로드에서 eventId, eventType 추출 시도 (로깅/인덱싱 목적)
            var eventId = "unknown-dlt-${System.currentTimeMillis()}"
            var eventType = "unknown"
            try {
                val jsonNode = objectMapper.readTree(originalPayload)
                // path().asText() 는 필드가 없거나 null일 때 기본값을 반환하여 안전함
                eventId = jsonNode.path("eventId").asText(eventId)
                eventType = jsonNode.path("type").asText(eventType) // 이벤트 페이로드의 타입 필드명 확인 필요
            } catch (e: Exception) {
                log.error("DLT 페이로드 JSON에서 eventId/eventType 파싱 실패: ${e.message}")
                // 기본값으로 진행
            }

            // Kafka 재시도 횟수는 헤더에서 직접 얻기 어려움. -1 마커 사용.
            val retryCountMarker = -1
            val failureSource = "KAFKA_CONSUMER" // 실패 지점 명시

            // RDB 저장을 위한 엔티티 생성
            val dlqEvent = DeadLetterTopicEvent.create(
                // id는 자동 생성
                eventId = eventId,
                eventType = eventType,
                retryCount = retryCountMarker,
                failureSource = failureSource, // 실패 지점 설정
                // 오류 메시지와 스택 트레이스 결합 (DB 컬럼 크기 고려)
                lastError = "Error: $exceptionMessage\nStackTrace: ${exceptionStacktrace}", // 길이 제한은 DeadLetterTopicEvent 엔티티에서 처리
                payload = originalPayload, // 전체 원본 페이로드 저장
            )

            // 데이터베이스에 저장
            deadLetterTopicRepository.save(dlqEvent)
            log.info(
                "DLT 메시지를 RDB에 성공적으로 저장: originalTopic=${originalTopic}, eventId=${eventId}, failureSource=${failureSource}, dbId=${dlqEvent.id}"
            )

            // RDB 저장 성공 후 Kafka DLT 메시지 Acknowledge
            ack.acknowledge()

        } catch (e: Exception) {
            log.error(
                "치명적 오류: DLT 메시지를 RDB에 저장 실패! 수동 조치 필요. originalTopic=${originalTopic}, key=${record.key()}, error=${e.message}",
                e
            )
        }
    }

    private fun getHeaderAsString(headers: Headers, headerName: String): String? {
        return headers.lastHeader(headerName)?.value()?.toString(StandardCharsets.UTF_8)
    }
}