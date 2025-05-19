package com.example.jobstat.core.core_event.dlt

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DeadLetterTopicRepository : JpaRepository<DeadLetterTopicEvent, Long> {
    fun findByEventId(eventId: String): DeadLetterTopicEvent?

    fun findByEventType(eventType: String): List<DeadLetterTopicEvent>

    @Query("SELECT DISTINCT e.eventType FROM DeadLetterTopicEvent e")
    fun findDistinctEventTypes(): List<String>
}
