// src/main/kotlin/com/example/jobstat/core/event/dlq/EventProcessingStatusRepository.kt
package com.example.jobstat.core.event.dlq

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventProcessingStatusRepository : JpaRepository<EventProcessingStatus, String> {
    fun findByProcessedFalseAndRetryCountLessThan(maxRetryCount: Int): List<EventProcessingStatus>

    @Query("SELECT e FROM EventProcessingStatus e WHERE e.processed = false AND e.lastProcessedAt < :cutoffTime")
    fun findUnprocessedBeforeTime(cutoffTime: LocalDateTime): List<EventProcessingStatus>

    fun findByEventType(eventType: String): List<EventProcessingStatus>
}