package com.example.jobstat.core.core_event.dlt

import com.example.jobstat.core.core_jpa_base.base.AuditableEntitySnow
import jakarta.persistence.*

@Entity
@Table(name = "dead_letter_topic")
class DeadLetterTopicEvent private constructor(
    @Column(nullable = false, length = 255, name = "event_id")
    val eventId: String,
    @Column(nullable = false, length = 100, name = "event_type")
    val eventType: String,
    @Column(nullable = false, name = "retry_count")
    val retryCount: Int,
    @Column(nullable = false, length = 50, name = "failure_source")
    val failureSource: String,
    @Column(nullable = true, length = 2000, name = "last_error")
    val lastError: String?,
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT", name = "payload")
    val payload: String,
) : AuditableEntitySnow() {
    companion object {
        fun create(
            eventId: String,
            eventType: String,
            retryCount: Int,
            failureSource: String,
            lastError: String?,
            payload: String,
        ): DeadLetterTopicEvent {
            require(eventId.isNotBlank()) { "이벤트 ID는 비어 있을 수 없습니다." }
            require(eventType.isNotBlank()) { "이벤트 타입은 비어 있을 수 없습니다." }
            require(failureSource.isNotBlank()) { "실패 출처는 비어 있을 수 없습니다." }
            require(payload.isNotBlank()) { "페이로드는 비어 있을 수 없습니다." }

            return DeadLetterTopicEvent(
                eventId = eventId,
                eventType = eventType,
                retryCount = retryCount,
                failureSource = failureSource,
                lastError = lastError?.take(2000),
                payload = payload,
            )
        }
    }
}
