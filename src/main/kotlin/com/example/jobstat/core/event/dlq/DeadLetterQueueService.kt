// src/main/kotlin/com/example/jobstat/core/event/dlq/DeadLetterQueueService.kt
package com.example.jobstat.core.event.dlq

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.navigator.EventHandlerRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 데드 레터 큐 서비스
 * 처리 실패한 이벤트 관리 및 재처리
 */
@Service
class DeadLetterQueueService(
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
    private val eventProcessingStatusRepository: EventProcessingStatusRepository,
    private val eventHandlerRegistry: EventHandlerRegistry,
    private val objectMapper: ObjectMapper
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 이벤트를 DLQ에 저장
     */
    @Transactional
    fun saveToDeadLetterQueue(event: Event<out EventPayload>, error: String, retryCount: Int) {
        try {
            val eventId = event.id.toString() // ID를 String으로 변환
            val payload = objectMapper.writeValueAsString(event)

            // 이미 DLQ에 있는지 확인
            if (deadLetterQueueRepository.findByEventId(eventId) != null) {
                log.info("이벤트가 이미 DLQ에 존재함: eventId={}", eventId)
                return
            }

            val dlqEvent = DeadLetterQueueEvent(
                eventId = eventId,
                eventType = event.type.name,
                retryCount = retryCount,
                lastError = error,
                payload = payload,
                shardKey = event.shardKey
            )

            deadLetterQueueRepository.save(dlqEvent)
            log.info("이벤트를 DLQ에 저장: eventId={}, eventType={}", eventId, event.type)
        } catch (e: Exception) {
            log.error("이벤트를 DLQ에 저장하는 중 오류 발생", e)
        }
    }

    /**
     * DLQ에서 이벤트 조회
     */
    fun getDeadLetterEvents(eventType: String): List<DeadLetterQueueEvent> {
        return deadLetterQueueRepository.findByEventType(eventType)
    }

    /**
     * DLQ에서 이벤트 재처리
     */
    @Transactional
    fun reprocessEvent(dlqEventId: Long): Boolean {
        try {
            val dlqEvent = deadLetterQueueRepository.findById(dlqEventId).orElse(null)
                ?: return false

            // 이벤트 페이로드 복원
            val event = objectMapper.readValue(dlqEvent.payload, Event::class.java)

            // 이벤트 핸들러 찾기
            val handler = eventHandlerRegistry.getHandler(event.type)
                ?: return false

            // 이벤트 재처리
            handler.handle(event)

            // 성공 시 DLQ에서 제거
            deadLetterQueueRepository.delete(dlqEvent)

            // EventProcessingStatus 업데이트
            val statusOptional = eventProcessingStatusRepository.findById(dlqEvent.eventId)
            if (statusOptional.isPresent) {
                val status = statusOptional.get()
                val updatedStatus = status.copy(
                    processed = true,
                    lastProcessedAt = java.time.LocalDateTime.now(),
                    lastError = "재처리 성공"
                )
                eventProcessingStatusRepository.save(updatedStatus)
            }

            log.info("DLQ 이벤트 재처리 성공: dlqEventId={}, eventId={}",
                dlqEventId, dlqEvent.eventId)

            return true
        } catch (e: Exception) {
            log.error("DLQ 이벤트 재처리 실패: dlqEventId={}", dlqEventId, e)
            return false
        }
    }

    /**
     * 특정 이벤트 유형의 모든 DLQ 이벤트 재처리
     */
    @Transactional
    fun reprocessEventsByType(eventType: String): Int {
        val events = deadLetterQueueRepository.findByEventType(eventType)
        var successCount = 0

        events.forEach { dlqEvent ->
            if (reprocessEvent(dlqEvent.id)) {
                successCount++
            }
        }

        return successCount
    }

    /**
     * 이벤트 ID로 DLQ 이벤트 조회
     */
    fun getByEventId(eventId: String): DeadLetterQueueEvent? {
        return deadLetterQueueRepository.findByEventId(eventId)
    }

    /**
     * DLQ에 있는 모든 이벤트 타입 조회
     */
    fun getEventTypes(): List<String> {
        return deadLetterQueueRepository.findDistinctEventTypes()
    }

    /**
     * DLQ 이벤트 직접 저장
     */
    fun save(dlqEvent: DeadLetterQueueEvent): DeadLetterQueueEvent {
        return deadLetterQueueRepository.save(dlqEvent)
    }
}