package com.example.jobstat.core.core_event.outbox

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository // 동일
interface OutboxRepository : JpaRepository<Outbox, Long> {
    fun findByRetryCountLessThanAndCreatedAtLessThanEqual(
        retryCount: Int,
        createdAt: LocalDateTime,
        pageable: Pageable,
    ): List<Outbox>
}
