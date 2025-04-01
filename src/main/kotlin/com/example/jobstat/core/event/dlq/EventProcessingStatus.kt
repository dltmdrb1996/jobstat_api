// src/main/kotlin/com/example/jobstat/core/event/dlq/EventProcessingStatus.kt
package com.example.jobstat.core.event.dlq

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 이벤트 처리 상태 엔티티
 * 이벤트 처리 과정을 추적하고 재시도 메커니즘을 지원
 */
@Entity
@Table(name = "event_processing_status")
data class EventProcessingStatus(
    @Id
    @Column(name = "event_id")
    val eventId: String,  // String으로 통일

    @Column(name = "event_type")
    val eventType: String,

    @Column(name = "processed")
    val processed: Boolean,

    @Column(name = "retry_count")
    val retryCount: Int = 0,

    @Column(name = "last_processed_at")
    val lastProcessedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_error", length = 1000)
    val lastError: String? = null,

    @Column(name = "shard_key")
    val shardKey: String? = null  // outbox 샤딩 키와 연계
)