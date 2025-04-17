package com.example.jobstat.core.event.outbox

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * 아웃박스 메시지 릴레이
 * 트랜잭션 이벤트를 수신하여 아웃박스에 저장하고, Kafka로 메시지를 발행합니다.
 * 1. 메인 트랜잭션 커밋 전에 아웃박스 레코드를 저장
 * 2. 메인 트랜잭션 커밋 후 즉시 비동기로 Kafka 발행 시도
 * 3. 스케줄러를 통해 미처리된 메시지 정기 점검 및 발행
 */
@Component
class OutboxMessageRelay(
    private val outboxRepository: OutboxRepository,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>,
    private val outboxProcessor: OutboxProcessor, // OutboxProcessor 주입
    @Value("\${outbox.relay.kafka-send-timeout-seconds:3}")
    private val kafkaSendTimeoutSeconds: Long,
    @Value("\${outbox.relay.scheduler.cutoff-seconds:10}")
    private val schedulerCutoffSeconds: Long,
    @Value("\${outbox.relay.scheduler.batch-size:100}")
    private val schedulerBatchSize: Int,
    @Value("\${outbox.relay.max-retry-count:3}")
    private val maxRetryCount: Int,
) : DisposableBean {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 메인 트랜잭션 커밋 전에 아웃박스 저장
     * 이 메소드에서 예외 발생 시 메인 트랜잭션도 롤백됩니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveOutboxOnEvent(outbox: Outbox) {
        log.info("아웃박스 저장 시도 (BEFORE_COMMIT 리스너): id=${outbox.id}, type=${outbox.eventType}")
        try {
            outboxRepository.save(outbox)
            log.info("아웃박스 저장 성공 (BEFORE_COMMIT 리스너): id=${outbox.id}, type=${outbox.eventType}")
        } catch (e: Exception) {
            log.error("아웃박스 저장 실패 (BEFORE_COMMIT 리스너): id=${outbox.id}, type=${outbox.eventType}, error=${e.message}", e)
            throw e
        }
    }

    /**
     * 메인 트랜잭션 커밋 후, Kafka로 즉시 발행 시도 (비동기 코루틴)
     * 실패 시 Outbox 레코드가 남아 스케줄러가 처리하게 됩니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishEventAsync(outbox: Outbox) {
        log.info("트랜잭션 커밋 확인, 즉시 발행 시도 (비동기): id=${outbox.id}, type=${outbox.eventType}")
        coroutineScope.launch {
            try {
                publishEventExecute(outbox)
            } catch (e: Exception) {
                log.error("비동기 즉시 발행 코루틴 실행 중 오류 발생: outboxId=${outbox.id}, error=${e.message}", e)
            }
        }
    }

    private suspend fun publishEventExecute(outbox: Outbox) {
        log.info("즉시 발행 실행 (코루틴): id=${outbox.id}, type=${outbox.eventType}, topic=${outbox.eventType.topic}")

        try {
            log.debug("Kafka 메시지 발행 시도(코루틴): id=${outbox.id}, topic=${outbox.eventType.topic}, payloadSize=${outbox.event.length}")

            withTimeout(kafkaSendTimeoutSeconds.toDuration(DurationUnit.SECONDS)) {
                outboxKafkaTemplate.send(outbox.eventType.topic, outbox.id.toString(), outbox.event).await()
            }

            log.info("즉시 발행 성공 (코루틴): id=${outbox.id}, type=${outbox.eventType}")

            withContext(Dispatchers.IO) {
                try {
                    outboxRepository.deleteById(outbox.id)
                    log.info("즉시 발행 성공 후 Outbox 삭제 완료: id=${outbox.id}")
                } catch (e: Exception) {
                    log.error("즉시 발행 성공 후 Outbox 삭제 실패 (컨슈머 멱등성 필요): id=${outbox.id}, error=${e.message}", e)
                }
            }
        } catch (e: TimeoutCancellationException) {
            log.warn("즉시 발행 실패 (타임아웃): id=${outbox.id}, type=${outbox.eventType}. 스케줄러가 재시도합니다.", e)
        } catch (e: Exception) {
            log.error("즉시 발행 실패 (오류): id=${outbox.id}, type=${outbox.eventType}, error=${e.message}. 스케줄러가 재시도합니다.", e)
        }
    }

    @Scheduled(
        fixedDelayString = "\${outbox.relay.scheduler.fixed-delay-seconds:10}000", // ms 단위
        initialDelayString = "\${outbox.relay.scheduler.initial-delay-seconds:5}000", // ms 단위
    )
    fun publishPendingEvents() {
        val cutoffTime = LocalDateTime.now().minusSeconds(schedulerCutoffSeconds)

        try {
            val pendingOutboxes =
                outboxRepository.findByRetryCountLessThanAndCreatedAtLessThanEqual(
                    maxRetryCount,
                    cutoffTime,
                    Pageable.ofSize(schedulerBatchSize),
                )

            if (pendingOutboxes.isNotEmpty()) {
                log.info("미처리 Outbox 메시지 발견: count=${pendingOutboxes.size}")

                pendingOutboxes.forEach { outbox ->
                    try {
                        // OutboxProcessor에 위임 (트랜잭션 관리 포함)
                        outboxProcessor.processOutboxItem(outbox)
                    } catch (itemError: Exception) {
                        log.error(
                            "개별 Outbox 항목 처리 중 트랜잭션 오류 발생 (스케줄러 레벨): id=${outbox.id}, type=${outbox.eventType}, error=${itemError.message}",
                            itemError,
                        )
                    }
                }
                log.info("미처리 Outbox 메시지 처리 완료 시도: processedCount=${pendingOutboxes.size}")
            }
        } catch (e: Exception) {
            log.error("미처리 Outbox 메시지 스캔/처리 중 오류 발생: error=${e.message}", e)
        }
    }

    override fun destroy() {
        log.info("OutboxMessageRelay 코루틴 스코프 취소 중...")
        coroutineScope.cancel()
        log.info("OutboxMessageRelay 코루틴 스코프 취소 완료.")
    }
}
