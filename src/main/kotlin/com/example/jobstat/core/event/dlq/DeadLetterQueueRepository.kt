// src/main/kotlin/com/example/jobstat/core/event/dlq/DeadLetterQueueRepository.kt
package com.example.jobstat.core.event.dlq

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DeadLetterQueueRepository : JpaRepository<DeadLetterQueueEvent, Long> {
    fun findByEventId(eventId: String): DeadLetterQueueEvent?
    fun findByEventType(eventType: String): List<DeadLetterQueueEvent>

    @Query("SELECT DISTINCT e.eventType FROM DeadLetterQueueEvent e")
    fun findDistinctEventTypes(): List<String>
}