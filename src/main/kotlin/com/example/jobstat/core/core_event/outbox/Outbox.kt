package com.example.jobstat.core.core_event.outbox

import com.example.jobstat.core.core_jpa_base.base.AuditableEntitySnow
import com.example.jobstat.core.core_event.model.EventType
import jakarta.persistence.*

@Entity
@Table(name = "outbox")
class Outbox protected constructor(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val eventType: EventType,
    @Column(name = "payload", columnDefinition = "TEXT")
    val event: String,
) : AuditableEntitySnow() {
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        internal set

    fun incrementRetryCount(): Int {
        this.retryCount++
        return this.retryCount
    }

    fun isMaxRetryExceeded(maxRetries: Int): Boolean = retryCount >= maxRetries

    companion object {
        fun create(
            eventType: EventType,
            event: String,
        ): Outbox {
            require(event.isNotBlank()) { "이벤트 페이로드는 비어 있을 수 없습니다." }

            return Outbox(
                eventType = eventType,
                event = event,
            )
        }
    }
}
