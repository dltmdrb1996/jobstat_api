package com.wildrew.jobstat.core.core_event.outbox // 패키지 변경

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.data.domain.PageRequest // Pageable.ofSize 대신 사용
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate // Spring Kafka 의존
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// import org.springframework.stereotype.Component // Auto-config에서 Bean으로 등록
// import org.springframework.beans.factory.annotation.Value // 생성자 주입으로 변경

// @Component // 제거
class OutboxMessageRelay(
    private val outboxRepository: OutboxRepository,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>,
    private val outboxProcessor: OutboxProcessor,
    private val coroutineScope: CoroutineScope, // @Qualifier("coreVirtualThreadScope") 등으로 명시적 주입 권장
    // @Value 제거, AutoConfiguration에서 주입
    private val kafkaSendTimeoutSeconds: Long,
    private val schedulerCutoffSeconds: Long,
    private val schedulerBatchSize: Int,
    private val maxRetryCountForScheduler: Int, // 프로퍼티 이름 명확화 (OutboxProcessor의 maxRetryCount와 구분)
    // 스케줄러 활성화 여부 (Auto-config에서 주입)
    private val schedulerEnabled: Boolean,
) : DisposableBean {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveOutboxOnEvent(outbox: Outbox) {
        // 이 리스너는 ApplicationEventPublisher.publishEvent(outbox) 호출 시 동작
        // 현재 트랜잭션이 있는지 확인 (없으면 Outbox 저장 의미 없음)
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn("트랜잭션 외부에서 Outbox 이벤트 발행 시도됨 (저장되지 않음): eventType={}", outbox.eventType)
            // 또는 예외를 던져서 발행자에게 알릴 수 있음
            // throw IllegalStateException("OutboxEventPublisher.publish must be called within an active transaction.")
            return
        }

        log.debug("아웃박스 저장 시도 (BEFORE_COMMIT): eventType={}", outbox.eventType) // ID는 아직 없음
        try {
            outboxRepository.save(outbox) // 여기서 ID가 생성됨 (JPA)
            log.debug("아웃박스 저장 성공 (BEFORE_COMMIT): outboxId={}, eventType={}", outbox.id, outbox.eventType)
        } catch (e: Exception) {
            log.error(
                "아웃박스 저장 실패 (BEFORE_COMMIT): eventType={}, error={}",
                outbox.eventType,
                e.message,
                e,
            )
            throw e // 예외를 전파하여 메인 트랜잭션 롤백 유도
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishEventAsyncAfterCommit(outbox: Outbox) {
        // 이 outbox 객체는 BEFORE_COMMIT에서 저장된, ID가 할당된 객체
        if (outbox.id == 0L) { // 저장되지 않은 객체 (ID가 할당되지 않음) - 혹시 모를 상황 대비
            log.warn("AFTER_COMMIT 리스너: ID가 할당되지 않은 Outbox 객체 수신. 즉시 발행 건너<0xEB><0x8E><0x84>: eventType={}", outbox.eventType)
            return
        }
        log.debug("트랜잭션 커밋 확인, 즉시 발행 시도 (비동기): outboxId={}, eventType={}", outbox.id, outbox.eventType)
        coroutineScope.launch {
            try {
                publishEventExecute(outbox)
            } catch (e: Exception) {
                // Coroutine 내부의 모든 예외
                log.error("비동기 즉시 발행 코루틴 실행 중 예상치 못한 오류: outboxId={}, error={}", outbox.id, e.message, e)
                // 이 예외는 이미 커밋된 트랜잭션에 영향을 주지 않음. Outbox에 남아 스케줄러가 처리.
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

            // 즉시 발행 성공 후 Outbox 삭제 (별도 트랜잭션 또는 트랜잭션 없이 시도)
            // 여기서는 @TransactionalEventListener(AFTER_COMMIT) 컨텍스트이므로,
            // 새로운 트랜잭션을 시작하거나, 트랜잭션 없이 delete 시도.
            // 만약 여기서 실패해도 메시지는 이미 발행되었으므로, 컨슈머 멱등성이 중요.
            try {
                // outboxRepository.deleteById(outbox.id) // 직접 삭제 시도
                // 또는 OutboxProcessor를 통해 트랜잭셔널하게 삭제 요청 (과할 수 있음)
                outboxRepository.deleteById(outbox.id)

                log.debug("즉시 발행 성공 후 Outbox 레코드(ID: {}) 삭제 완료", outbox.id)
            } catch (e: Exception) {
                log.error(
                    "즉시 발행은 성공했으나 Outbox 레코드(ID: {}) 삭제 실패 (컨슈머 멱등성 중요). Error: {}",
                    outbox.id,
                    e.message,
                    e,
                )
                // 메시지는 이미 발행되었으므로, 이 오류는 로깅만 하고 무시.
            }
        } catch (e: TimeoutCancellationException) {
            log.warn(
                "즉시 발행 실패 (타임아웃): outboxId={}, type={}. 스케줄러가 재시도합니다.",
                outbox.id,
                outbox.eventType,
                e,
            )
        } catch (e: Exception) {
            // Kafka 발행 관련 모든 예외
            log.error(
                "즉시 발행 실패 (오류): outboxId={}, type={}, error={}. 스케줄러가 재시도합니다.",
                outbox.id,
                outbox.eventType,
                e.message,
                e,
            )
        }
    }

    // @Scheduled 어노테이션은 이 클래스에 유지하고, Auto-config에서 schedulerEnabled 프로퍼티로 제어
    @Scheduled(
        fixedDelayString = "\${jobstat.core.event.outbox.relay.scheduler.fixed-delay-millis:10000}",
        initialDelayString = "\${jobstat.core.event.outbox.relay.scheduler.initial-delay-millis:5000}",
    )
    fun processPendingOutboxMessagesByScheduler() { // 메소드 이름 변경
        if (!schedulerEnabled) { // Auto-config에서 주입받은 프로퍼티로 활성화 여부 체크
            return
        }
//        log.debug("미처리 Outbox 메시지 스케줄러 실행 시작...")
        val cutoffTime = LocalDateTime.now().minusSeconds(schedulerCutoffSeconds)
        try {
            // Pageable.ofSize 대신 PageRequest.of 사용
            val pageable: Pageable = PageRequest.of(0, schedulerBatchSize)
            val pendingOutboxes =
                outboxRepository.findByRetryCountLessThanAndCreatedAtLessThanEqual(
                    maxRetryCountForScheduler, // 스케줄러용 최대 재시도 횟수
                    cutoffTime,
                    pageable,
                )

            if (pendingOutboxes.isNotEmpty()) {
//                log.info("미처리 Outbox 메시지 발견: count={}", pendingOutboxes.size)
                pendingOutboxes.forEach { outbox ->
                    try {
                        // OutboxProcessor에 위임 (각각의 아이템은 새 트랜잭션으로 처리됨)
                        outboxProcessor.processOutboxItem(outbox)
                    } catch (itemError: Exception) {
                        // OutboxProcessor.processOutboxItem에서 예외가 여기까지 전파되었다면,
                        // 해당 아이템 처리에 심각한 문제가 발생한 것 (예: DB 연결 불가).
                        // 트랜잭션은 이미 롤백되었을 것임.
                        log.error(
                            "개별 Outbox 항목(ID: {}) 처리 중 스케줄러 레벨에서 오류 감지 (이미 롤백되었을 수 있음). Error: {}",
                            outbox.id,
                            itemError.message,
                            itemError,
                        )
                        // 개별 아이템 실패가 다른 아이템 처리에 영향 주지 않도록 continue
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
