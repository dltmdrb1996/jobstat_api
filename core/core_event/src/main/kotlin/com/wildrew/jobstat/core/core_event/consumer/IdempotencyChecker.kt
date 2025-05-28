package com.wildrew.jobstat.core.core_event.consumer

import java.time.Duration

interface IdempotencyChecker {
    /**
     * eventId (또는 batchId)가 이미 처리되었는지 확인합니다.
     * @param key 멱등성 확인을 위한 고유 키 (예: eventId, batchId)
     * @return 이미 처리되었으면 true, 아니면 false
     */
    fun isAlreadyProcessed(key: Long): Boolean

    /**
     * eventId (또는 batchId)를 처리됨으로 마킹합니다.
     * @param key 멱등성 확인을 위한 고유 키
     * @param ttl 해당 키의 만료 시간
     */
    fun markAsProcessed(
        key: Long,
        ttl: Duration,
    )
}
