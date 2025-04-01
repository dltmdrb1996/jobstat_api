// src/main/kotlin/com/example/jobstat/core/event/dlq/DlqRetryService.kt
package com.example.jobstat.core.event.dlq

import com.example.jobstat.core.event.outbox.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * DLQ 재처리 서비스
 * DLQ에 있는 이벤트를 주기적으로 재처리 시도
 */
@Service
class DlqRetryService(
    private val deadLetterQueueService: DeadLetterQueueService,
    private val eventProcessingStatusRepository: EventProcessingStatusRepository,
    private val outboxRepository: OutboxRepository
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    
    /**
     * 주기적으로 DLQ 이벤트 재처리 시도
     */
    @Scheduled(cron = "0 */30 * * * *") // 30분마다 실행
    fun retryDeadLetterQueueEvents() {
        log.info("DLQ 이벤트 재처리 작업 시작")
        
        val eventTypes = deadLetterQueueService.getEventTypes()
        var totalSuccess = 0
        var totalFailed = 0
        
        eventTypes.forEach { eventType ->
            try {
                val successCount = deadLetterQueueService.reprocessEventsByType(eventType)
                log.info("이벤트 타입 {} 재처리 결과: 성공={}", eventType, successCount)
                totalSuccess += successCount
            } catch (e: Exception) {
                log.error("이벤트 타입 {} 재처리 중 오류 발생", eventType, e)
                totalFailed++
            }
        }
        
        log.info("DLQ 이벤트 재처리 작업 완료: 성공={}, 실패={}", totalSuccess, totalFailed)
    }
    
    /**
     * 일정 시간 동안 처리되지 않은 이벤트 상태 확인 및 정리
     */
    @Scheduled(cron = "0 */15 * * * *") // 15분마다 실행
    fun checkStalledEvents() {
        log.info("처리되지 않은 이벤트 확인 작업 시작")
        
        val cutoffTime = LocalDateTime.now().minusHours(1)
        val stalledEvents = eventProcessingStatusRepository.findUnprocessedBeforeTime(cutoffTime)
        
        log.info("1시간 이상 처리되지 않은 이벤트 수: {}", stalledEvents.size)
        
        stalledEvents.forEach { status ->
            try {
                // Outbox에서 원본 이벤트 찾기
                val outboxOptional = outboxRepository.findByEventId(status.eventId)
                
                if (outboxOptional.isPresent) {
                    // Outbox에 있으면 상태 정리 - 아직 발행되지 않은 상태
                    val outbox = outboxOptional.get()
                    if (!outbox.processed) {
                        log.info("미처리 이벤트(outbox에 있음): eventId={}", status.eventId)
                        
                        // 상태 업데이트 - 아직 처리해야 함
                        val updatedStatus = status.copy(
                            lastProcessedAt = LocalDateTime.now(),
                            lastError = "Outbox에서 아직 처리되지 않음"
                        )
                        eventProcessingStatusRepository.save(updatedStatus)
                    }
                } else {
                    // Outbox에 없으면 DLQ로 이동 - 처리에 실패했거나 유실된 상태
                    log.warn("미처리 이벤트(outbox에 없음) - DLQ로 이동: eventId={}", status.eventId)
                    
                    // DLQ 엔트리 생성 (페이로드는 알 수 없으므로 null)
                    val dlqEvent = DeadLetterQueueEvent(
                        eventId = status.eventId,
                        eventType = status.eventType,
                        retryCount = status.retryCount,
                        lastError = "이벤트 처리 실패 또는 유실",
                        payload = "{}", // 빈 JSON
                        shardKey = status.shardKey
                    )
                    
                    // 이미 DLQ에 없는 경우에만 저장
                    if (deadLetterQueueService.getByEventId(status.eventId) == null) {
                        deadLetterQueueService.save(dlqEvent)
                    }
                    
                    // 상태 업데이트 - 처리 완료로 표시
                    val updatedStatus = status.copy(
                        processed = true,
                        lastProcessedAt = LocalDateTime.now(),
                        lastError = "DLQ로 이동 (이벤트 유실)"
                    )
                    eventProcessingStatusRepository.save(updatedStatus)
                }
            } catch (e: Exception) {
                log.error("미처리 이벤트 확인 중 오류 발생: eventId={}", status.eventId, e)
            }
        }
        
        log.info("처리되지 않은 이벤트 확인 작업 완료")
    }
}