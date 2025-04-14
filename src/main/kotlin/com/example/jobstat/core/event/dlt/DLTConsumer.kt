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

/**
 * 데드 레터 토픽 컨슈머
 * DLT(Dead Letter Topic)로 전송된 실패 메시지를 수신하여 처리합니다.
 * 실패한 메시지는 데이터베이스에 저장되어 추후 분석 및 복구에 활용됩니다.
 */
@Component
class DLTConsumer(
    private val deadLetterTopicRepository: DeadLetterTopicRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    // ===================================================
    // Kafka 리스너 메소드
    // ===================================================

    /**
     * 모든 .DLT 토픽의 메시지를 수신하여 처리합니다.
     * 수신된 메시지는 데이터베이스에 저장됩니다.
     * @param record Kafka 컨슈머 레코드
     * @param ack Kafka 확인응답(Acknowledgment) 객체
     */
    @KafkaListener(
        topicPattern = ".*\\.DLT",
        groupId = "\${dlt.consumer.group-id:dlt-persistence-group}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun processDltMessage(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        // Kafka 헤더에서 메타데이터 추출
        val headers = record.headers()
        val originalTopic = getHeaderAsString(headers, KafkaHeaders.ORIGINAL_TOPIC) ?: "unknown"
        val exceptionMessage = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_MESSAGE) ?: "N/A"
        // 스택 트레이스는 매우 길 수 있으므로 길이 제한
        val exceptionStacktrace = getHeaderAsString(headers, KafkaHeaders.EXCEPTION_STACKTRACE)?.take(1500) ?: "N/A"
        val originalPayload = record.value() // 원본 이벤트 페이로드

        log.warn(
            "DLT 메시지 수신: originalTopic=$originalTopic, key=${record.key()}, partition=${record.partition()}, offset=${record.offset()}, error='$exceptionMessage'",
        )
        log.debug("DLT 예외 스택 트레이스: $exceptionStacktrace") // 필요시 DEBUG 레벨로 스택트레이스 확인

        try {
            // 이벤트 메타데이터 추출
            val (eventId, eventType) = extractEventMetadata(originalPayload)

            // 실패 정보 설정
            val retryCountMarker = -1 // Kafka 재시도 횟수는 직접 얻기 어려움
            val failureSource = "KAFKA_CONSUMER" // 실패 지점 명시
            val errorDetail = "오류: $exceptionMessage\n스택트레이스: $exceptionStacktrace"

            // DLT 이벤트 엔티티 생성 및 저장
            val dltEvent =
                createAndSaveDltEvent(
                    eventId,
                    eventType,
                    retryCountMarker,
                    failureSource,
                    errorDetail,
                    originalPayload,
                )

            log.info(
                "DLT 메시지를 DB에 성공적으로 저장: originalTopic=$originalTopic, eventId=$eventId, failureSource=$failureSource, dbId=${dltEvent.id}",
            )

            // 저장 성공 후 Kafka 메시지 확인응답
            ack.acknowledge()
        } catch (e: Exception) {
            log.error(
                "치명적 오류: DLT 메시지를 DB에 저장 실패! 수동 조치 필요. originalTopic=$originalTopic, key=${record.key()}, error=${e.message}",
                e,
            )
        }
    }

    // ===================================================
    // 유틸리티 메소드
    // ===================================================

    /**
     * 이벤트 페이로드에서 메타데이터(이벤트 ID, 이벤트 타입)를 추출합니다.
     *
     * @param payload 이벤트 페이로드(JSON 형식)
     * @return 이벤트 ID와 이벤트 타입의 Pair
     */
    private fun extractEventMetadata(payload: String): Pair<String, String> {
        var eventId = "unknown-dlt-${System.currentTimeMillis()}"
        var eventType = "unknown"

        try {
            val jsonNode = objectMapper.readTree(payload)
            // path().asText()는 필드가 없거나 null일 때 기본값을 반환하여 안전함
            eventId = jsonNode.path("eventId").asText(eventId)
            eventType = jsonNode.path("type").asText(eventType) // 이벤트 페이로드의 타입 필드명 확인 필요
        } catch (e: Exception) {
            log.error("DLT 페이로드 JSON에서 eventId/eventType 파싱 실패: ${e.message}")
            // 기본값으로 진행
        }

        return Pair(eventId, eventType)
    }

    /**
     * DLT 이벤트를 생성하고 저장합니다.
     *
     * @param eventId 이벤트 ID
     * @param eventType 이벤트 타입
     * @param retryCount 재시도 횟수
     * @param failureSource 실패 출처
     * @param errorDetail 오류 상세 내용
     * @param payload 원본 페이로드
     * @return 저장된 DLT 이벤트 엔티티
     */
    private fun createAndSaveDltEvent(
        eventId: String,
        eventType: String,
        retryCount: Int,
        failureSource: String,
        errorDetail: String,
        payload: String,
    ): DeadLetterTopicEvent {
        // DLT 이벤트 엔티티 생성
        val dltEvent =
            DeadLetterTopicEvent.create(
                eventId = eventId,
                eventType = eventType,
                retryCount = retryCount,
                failureSource = failureSource,
                lastError = errorDetail, // 길이 제한은 DeadLetterTopicEvent 엔티티에서 처리
                payload = payload, // 전체 원본 페이로드 저장
            )

        // 데이터베이스에 저장
        return deadLetterTopicRepository.save(dltEvent)
    }

    /**
     * Kafka 헤더에서 문자열 값을 추출합니다.
     *
     * @param headers Kafka 헤더
     * @param headerName 헤더 이름
     * @return 헤더 값 또는 null
     */
    private fun getHeaderAsString(
        headers: Headers,
        headerName: String,
    ): String? = headers.lastHeader(headerName)?.value()?.toString(StandardCharsets.UTF_8)
}
