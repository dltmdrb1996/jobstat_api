package com.example.jobstat.core.core_id_generator // 실제 패키지 경로

import org.slf4j.LoggerFactory
import kotlin.math.max

class SynchronizedSnowflake(
    private val nodeId: Long = 0L,
) : SnowflakeGenerator {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // 표준 Snowflake 비트 구성
        private const val UNUSED_BITS = 1
        private const val EPOCH_BITS = 41
        private const val NODE_ID_BITS = 10
        private const val SEQUENCE_BITS = 12 // 표준 12비트 사용

        const val MAX_NODE_ID = (1L shl NODE_ID_BITS) - 1
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS) - 1

        // 비트 쉬프트 상수
        private const val TIMESTAMP_SHIFT = NODE_ID_BITS + SEQUENCE_BITS // 22
        private const val NODE_ID_SHIFT = SEQUENCE_BITS // 12

        // 기준 시각 (Epoch) - UTC 2024-01-01T00:00:00Z
        private const val DEFAULT_EPOCH_MILLIS = 1704067200000L
        private const val WAIT_SLEEP_MILLIS = 1L // waitNextMillis sleep 시간
    }

    private val epochMillis = DEFAULT_EPOCH_MILLIS

    // 상태 변수
    private var lastTimestamp: Long = -1L
    private var sequence: Long = 0L

    // 동기화를 위한 락 객체
    private val lock = Any()

    init {
        require(nodeId in 0..MAX_NODE_ID) {
            "Node ID must be between 0 and $MAX_NODE_ID. Provided: $nodeId"
        }
        log.debug("Initialized SynchronizedSnowflakeGenerator with Node ID: {}", nodeId)
    }

    /**
     * 다음 Snowflake ID를 thread-safe하게 생성하여 반환한다.
     */
    override fun nextId(): Long {
        val currentMillis = System.currentTimeMillis()
        val resultTimestamp: Long
        val resultSequence: Long

        synchronized(lock) {
            var now = currentMillis - epochMillis
            now = max(now, lastTimestamp) // 시계 역행 방지

            if (now == lastTimestamp) {
                sequence = (sequence + 1) and MAX_SEQUENCE // 12비트 기준
                if (sequence == 0L) {
                    now = waitNextMillis(lastTimestamp)
                }
            } else {
                sequence = 0L
            }
            lastTimestamp = now
            resultTimestamp = lastTimestamp
            resultSequence = sequence
        }

        return (
            (resultTimestamp shl TIMESTAMP_SHIFT)
                or (nodeId shl NODE_ID_SHIFT)
                or resultSequence
        )
    }

    private fun waitNextMillis(currentLastTimestamp: Long): Long {
        var timestamp: Long
        do {
            try {
                Thread.sleep(WAIT_SLEEP_MILLIS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("waitNextMillis interrupted, retrying.", e)
            }
            timestamp = (System.currentTimeMillis() - epochMillis)
        } while (timestamp <= currentLastTimestamp)
        return timestamp
    }
}
