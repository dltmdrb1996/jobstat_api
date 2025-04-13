package com.example.jobstat.core.event.outbox

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OutboxRepository : JpaRepository<Outbox, Long> {

    // 특정 시간 이전에 생성된 처리되지 않은 이벤트 조회
    fun findByRetryCountLessThanAndCreatedAtLessThanEqual(
        retryCount: Int,
        createdAt: LocalDateTime,
        pageable: Pageable
    ): List<Outbox>

}