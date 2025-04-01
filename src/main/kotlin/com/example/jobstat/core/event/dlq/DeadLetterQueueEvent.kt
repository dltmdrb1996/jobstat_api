// src/main/kotlin/com/example/jobstat/core/event/dlq/DeadLetterQueueEvent.kt
package com.example.jobstat.core.event.dlq

import com.example.jobstat.core.base.BaseIdEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 데드 레터 큐 이벤트 엔티티
 * 최종적으로 처리에 실패한 이벤트를 저장하는 DLQ
 */
@Entity
@Table(name = "dead_letter_queue")
data class DeadLetterQueueEvent(
    override val id: Long = 0,

    @Column(name = "event_id", unique = true)
    val eventId: String,  // String으로 통일

    @Column(name = "event_type")
    val eventType: String,

    @Column(name = "retry_count")
    val retryCount: Int,

    @Column(name = "last_error", length = 1000)
    val lastError: String?,

    @Column(name = "payload", length = 10000)
    val payload: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "shard_key")
    val shardKey: String? = null  // outbox 샤딩 키와 연계
) : BaseIdEntity()