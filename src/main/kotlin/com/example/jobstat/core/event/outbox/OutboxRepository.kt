package com.example.jobstat.core.event.outbox

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OutboxRepository : JpaRepository<Outbox, Long> {
    fun findByRetryCountLessThanAndCreatedAtLessThanEqual(
        retryCount: Int,
        createdAt: LocalDateTime,
        pageable: Pageable,
    ): List<Outbox>
}
