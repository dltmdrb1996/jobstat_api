package com.wildrew.jobstat.core.core_event.outbox

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class OutboxMessageRelay(
    private val outboxRepository: OutboxRepository,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>,
    private val outboxProcessor: OutboxProcessor,
    private val coroutineScope: CoroutineScope,
    private val kafkaSendTimeoutSeconds: Long,
    private val schedulerCutoffSeconds: Long,
    private val schedulerBatchSize: Int,
    private val maxRetryCountForScheduler: Int,
    private val schedulerEnabled: Boolean,
) : DisposableBean {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveOutboxOnEvent(outbox: Outbox) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn("트랜잭션 외부에서 Outbox 이벤트 발행 시도됨 (저장되지 않음): eventType={}", outbox.eventType)
            return
        }

        log.debug("아웃박스 저장 시도 (BEFORE_COMMIT): eventType={}", outbox.eventType)
        try {
            outboxRepository.save(outbox)
            log.debug("아웃박스 저장 성공 (BEFORE_COMMIT): outboxId={}, eventType={}", outbox.id, outbox.eventType)
        } catch (e: Exception) {
            log.error(
                "아웃박스 저장 실패 (BEFORE_COMMIT): eventType={}, error={}",
                outbox.eventType,
                e.message,
                e,
            )
            throw e
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishEventAsyncAfterCommit(outbox: Outbox) {
        if (outbox.id == 0L) {
            log.warn("AFTER_COMMIT 리스너: ID가 할당되지 않은 Outbox 객체 수신. 즉시 발행 건너<0xEB><0x8E><0x84>: eventType={}", outbox.eventType)
            return
        }
        log.debug("트랜잭션 커밋 확인, 즉시 발행 시도 (비동기): outboxId={}, eventType={}", outbox.id, outbox.eventType)
        coroutineScope.launch {
            try {
                publishEventExecute(outbox)
            } catch (e: Exception) {
                log.error("비동기 즉시 발행 코루틴 실행 중 예상치 못한 오류: outboxId={}, error={}", outbox.id, e.message, e)
            }
        }
    }

    private suspend fun publishEventExecute(outbox: Outbox) {
        log.debug("즉시 발행 실행 (코루틴): outboxId={}, type={}, topic={}", outbox.id, outbox.eventType, outbox.eventType.getTopicName())
        try {
            withTimeout(kafkaSendTimeoutSeconds.toDuration(DurationUnit.SECONDS)) {
                outboxKafkaTemplate.send(outbox.eventType.getTopicName(), outbox.id.toString(), outbox.event).await()
            }
            log.info("즉시 발행 성공 (코루틴): outboxId={}, type={}", outbox.id, outbox.eventType)

            try {
                outboxRepository.deleteById(outbox.id)

                log.debug("즉시 발행 성공 후 Outbox 레코드(ID: {}) 삭제 완료", outbox.id)
            } catch (e: Exception) {
                log.error(
                    "즉시 발행은 성공했으나 Outbox 레코드(ID: {}) 삭제 실패 (컨슈머 멱등성 중요). Error: {}",
                    outbox.id,
                    e.message,
                    e,
                )
            }
        } catch (e: TimeoutCancellationException) {
            log.warn(
                "즉시 발행 실패 (타임아웃): outboxId={}, type={}. 스케줄러가 재시도합니다.",
                outbox.id,
                outbox.eventType,
                e,
            )
        } catch (e: Exception) {
            log.error(
                "즉시 발행 실패 (오류): outboxId={}, type={}, error={}. 스케줄러가 재시도합니다.",
                outbox.id,
                outbox.eventType,
                e.message,
                e,
            )
        }
    }

    @Scheduled(
        fixedDelayString = "\${jobstat.core.event.outbox.relay.scheduler.fixed-delay-millis:10000}",
        initialDelayString = "\${jobstat.core.event.outbox.relay.scheduler.initial-delay-millis:5000}",
    )
    fun processPendingOutboxMessagesByScheduler() {
        if (!schedulerEnabled) {
            return
        }
        val cutoffTime = LocalDateTime.now().minusSeconds(schedulerCutoffSeconds)
        try {
            val pageable: Pageable = PageRequest.of(0, schedulerBatchSize)
            val pendingOutboxes =
                outboxRepository.findByRetryCountLessThanAndCreatedAtLessThanEqual(
                    maxRetryCountForScheduler,
                    cutoffTime,
                    pageable,
                )

            if (pendingOutboxes.isNotEmpty()) {
                pendingOutboxes.forEach { outbox ->
                    try {
                        outboxProcessor.processOutboxItem(outbox)
                    } catch (itemError: Exception) {
                        log.error(
                            "개별 Outbox 항목(ID: {}) 처리 중 스케줄러 레벨에서 오류 감지 (이미 롤백되었을 수 있음). Error: {}",
                            outbox.id,
                            itemError.message,
                            itemError,
                        )
                    }
                }
//                log.info("미처리 Outbox 메시지 {}개에 대한 처리 시도 완료", pendingOutboxes.size)
            } else {
//                log.debug("처리 대기 중인 Outbox 메시지 없음.")
            }
        } catch (e: Exception) {
            // DB 조회 실패 등 스케줄러 실행 자체의 오류
            log.error("미처리 Outbox 메시지 스캔/처리 중 스케줄러 오류 발생: error={}", e.message, e)
        }
    }

    override fun destroy() {
        log.info("OutboxMessageRelay 코루틴 스코프 취소 중...")
        if (coroutineScope.isActive) {
            coroutineScope.cancel("OutboxMessageRelay is being destroyed.")
        }
        log.info("OutboxMessageRelay 코루틴 스코프 취소 완료.")
    }
}
