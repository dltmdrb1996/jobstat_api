package com.example.jobstat.core.event.outbox

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class OutboxProcessor(
    private val outboxRepository: OutboxRepository,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>,

    // --- 설정값 주입 ---
    @Value("\${outbox.processor.kafka-send-timeout-seconds:5}") // 기본값 5초 (스케줄러는 조금 더 길게)
    private val kafkaSendTimeoutSeconds: Long,

    @Value("\${outbox.processor.max-retry-count:5}") // 기본값 5
    private val maxRetryCount: Int

) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 단일 Outbox 항목 처리 - 별도 트랜잭션으로 실행됩니다.
     * Kafka로 재전송을 시도합니다.
     * 실패 시 재시도 횟수를 증가시키거나, 최대 횟수 초과 시 로그 기록 후 Outbox 항목을 삭제합니다.
     */
    @Transactional // 이 메서드 전체가 하나의 트랜잭션으로 묶입니다.
    fun processOutboxItem(outbox: Outbox) {
        log.info("Outbox 처리 시작 (스케줄러): id=${outbox.id}, type=${outbox.eventType}, retryCount=${outbox.retryCount}")
        // RDB DLQ 로직 제거됨 - Kafka 전송 시도
        retryEventExecute(outbox)
    }

    /**
     * 이벤트 재시도 실행 (Kafka 전송 시도).
     * 성공 시 Outbox 레코드 삭제, 실패 시 재시도 실패 처리 로직 호출.
     * `processOutboxItem`의 트랜잭션 내에서 호출됩니다.
     */
    private fun retryEventExecute(outbox: Outbox) {
        try {
            log.debug("Kafka 메시지 재전송 시도: id=${outbox.id}, topic=${outbox.eventType.topic}")

            // Kafka 메시지 전송 (동기 방식 사용 - @Transactional 내부)
            val sendResult = outboxKafkaTemplate.send(
                outbox.eventType.topic,
                outbox.id.toString(), // key로 id 사용 고려
                outbox.event
            ).get(kafkaSendTimeoutSeconds, TimeUnit.SECONDS) // 설정된 타임아웃 사용

            // 전송 결과 로깅 (선택적)
            val recordMetadata = sendResult.recordMetadata
            log.debug("Kafka 재전송 응답 수신: id=${outbox.id}, partition=${recordMetadata?.partition()}, offset=${recordMetadata?.offset()}")

            // 성공 시 Outbox 삭제 (같은 트랜잭션 내)
            outboxRepository.deleteById(outbox.id)

            log.info(
                "이벤트 재시도 성공 및 Outbox 삭제: id=${outbox.id}, type=${outbox.eventType}, retryCount=${outbox.retryCount}"
            )

        } catch (e: Exception) {
            // 타임아웃 포함 Kafka 전송 관련 모든 예외 처리
            val errorMessage = when (e) {
                is TimeoutException -> "Kafka 전송 확인 대기 중 타임아웃"
                else -> e.message ?: "알 수 없는 Kafka 전송 오류"
            }
            log.error(
                "이벤트 재시도 실패: id=${outbox.id}, type=${outbox.eventType}, retryCount=${outbox.retryCount}, error='${errorMessage}'"
            )
            // 실패 처리 (재시도 횟수 증가 또는 최대 횟수 초과 처리)
            processFailedRetry(outbox, errorMessage)
        }
    }

    /**
     * Kafka 재전송 실패 시 처리 로직.
     * 재시도 횟수를 증가시키거나, 최대 횟수 초과 시 로그 남기고 Outbox 레코드 삭제.
     * `processOutboxItem`의 트랜잭션 내에서 호출됩니다.
     *
     * @param outbox 실패한 Outbox 항목
     * @param errorMessage 실패 원인 메시지
     */
    private fun processFailedRetry(outbox: Outbox, errorMessage: String) {
        try {
            val nextRetryCount = outbox.retryCount + 1

            if (nextRetryCount >= maxRetryCount) { // '>=' 로 변경하여 maxRetryCount 도달 시 처리
                log.warn(
                    "재시도 실패 후 최대 횟수 도달: id=${outbox.id}, finalRetryCount=${nextRetryCount}. RDB DLQ 대신 오류 로깅 후 Outbox 레코드 삭제."
                )
                // *** 중요: 최대 재시도 도달 시 Outbox 메시지 삭제 ***
                // Kafka로의 전송 자체가 계속 실패하는 상황.
                // 시스템 관리자가 로그를 보고 원인 파악 및 수동 조치 필요.
                // 여기서 삭제해야 스케줄러가 계속 이 메시지를 처리하려 시도하지 않음.
                outboxRepository.deleteById(outbox.id)
                log.error(
                    "치명적 오류: ${maxRetryCount}번의 재시도 후 Kafka 전송 실패 및 Outbox 메시지 삭제됨. 수동 확인 필요. OutboxId=${outbox.id}, Type=${outbox.eventType}, Reason='${errorMessage.take(500)}'"
                )

            } else {
                // 재시도 횟수 업데이트 후 저장 (같은 트랜잭션 내)
                // copy()를 사용하여 불변성 유지 및 명시적 업데이트
                val updatedOutbox = outbox.copy(retryCount = nextRetryCount)
                outboxRepository.save(updatedOutbox)
                log.info(
                    "재시도 횟수 증가: id=${updatedOutbox.id}, type=${updatedOutbox.eventType}, newRetryCount=${updatedOutbox.retryCount}"
                )
            }
        } catch (e: Exception) {
            log.error("재시도 실패 처리 중 심각한 오류 발생 (트랜잭션 롤백 가능성 있음): id=${outbox.id}, error=${e.message}", e)
            // 이 예외는 상위 @Transactional 에 의해 처리되어 롤백될 수 있습니다.
            throw e // 예외를 다시 던져 트랜잭션 롤백 유도
        }
    }
}