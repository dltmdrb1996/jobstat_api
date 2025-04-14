package com.example.jobstat.core.global.utils.id_generator.sharded // 내부 구현용 패키지

import org.slf4j.LoggerFactory
import kotlin.math.max

/**
 * ShardedSnowflake 내부에서 사용할 코어 ID 생성 로직.
 * - Synchronized 적용으로 정확성 보장
 * - nextTimestampAndSequence는 (timestamp << sequenceBits | sequence) 값 반환
 * - sequenceBits를 외부에서 주입받음
 */
internal class SynchronizedSnowflakeCore(
    private val nodeIdForLog: Long, // 로깅이나 예외 메시지 표시용
    private val sequenceBits: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // 공통 상수
        private const val EPOCH_BITS = 41 // 타임스탬프 비트 수 (내부 계산용)
        private const val NODE_ID_BITS_FOR_LOG = 10 // 로깅용 노드 ID 비트 수 (외부에서 가져와도 됨)
        private const val DEFAULT_EPOCH_MILLIS = 1704067200000L // UTC 2024-01-01T00:00:00Z
        private const val WAIT_SLEEP_MILLIS = 1L // waitNextMillis sleep 시간
    }

    private val epochMillis = DEFAULT_EPOCH_MILLIS
    private val maxSequence = (1L shl sequenceBits) - 1 // 주입된 비트 수로 maxSequence 계산

    // 상태 변수
    private var lastTimestamp: Long = -1L
    private var sequence: Long = 0L

    init {
        // sequenceBits 유효성 검사 등 필요시 추가
        log.trace("Initialized SynchronizedSnowflakeCore instance for Node [{}], SequenceBits: {}", nodeIdForLog, sequenceBits)
    }

    /**
     * 해당 코어의 (timestamp << sequenceBits | sequence) 값을 반환 (thread-safe)
     */
    @Synchronized
    fun nextTimestampAndSequence(): Long {
        var currentMillis = System.currentTimeMillis()
        var now = currentMillis - epochMillis
        now = max(now, lastTimestamp) // 시계 역행 방지

        if (now == lastTimestamp) {
            sequence = (sequence + 1) and maxSequence
            if (sequence == 0L) {
                now = waitNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }
        lastTimestamp = now

        // (timestamp | sequence) 부분만 조합하여 반환
        return (lastTimestamp shl sequenceBits) or sequence
    }

    private fun waitNextMillis(currentLastTimestamp: Long): Long {
        var timestamp: Long
        do {
            try {
                Thread.sleep(WAIT_SLEEP_MILLIS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Node [{}] Core [{}] waitNextMillis interrupted, retrying.", nodeIdForLog, this.hashCode(), e)
            }
            timestamp = (System.currentTimeMillis() - epochMillis)
        } while (timestamp <= currentLastTimestamp)
        return timestamp
    }
}
