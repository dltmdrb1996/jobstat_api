package com.example.jobstat.core.global.utils

import java.util.concurrent.atomic.AtomicLong

/**
 * 기존 Snowflake 알고리즘을 CAS(Compare-And-Set) 기반으로 개선
 * 
 * 비트 배분 (총 64bit)
 *  - 1bit   : 미사용 (최상위 비트)
 *  - 41bits : 시간(Epoch)  
 *  - 10bits : 노드 ID
 *  - 12bits : 시퀀스(sequence)
 */
class Snowflake(
    /**
     * Snowflake에서는 보통 고정된 Node ID를 사용.
     * 여기서는 임의로 지정할 수 있도록 열어두되, 기본값은 0L
     * (필요하면 외부에서 주입해서 서버별 혹은 노드별로 구분 가능)
     */
    private val nodeId: Long = 0L
) {

    companion object {
        private const val UNUSED_BITS = 1
        private const val EPOCH_BITS = 41
        private const val NODE_ID_BITS = 10
        private const val SEQUENCE_BITS = 12

        private const val MAX_NODE_ID = (1L shl NODE_ID_BITS) - 1
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS) - 1

        // UTC 기준 2024-01-01T00:00:00Z
        private const val DEFAULT_START_TIME_MILLIS = 1704067200000L
    }

    // Snowflake 시작 기준 시각
    private val epochStartMillis = DEFAULT_START_TIME_MILLIS

    // nodeId가 범위 넘어가면 예외
    init {
        require(nodeId in 0..MAX_NODE_ID) {
            "nodeId must be between 0 and $MAX_NODE_ID"
        }
    }

    /**
     * timestamp(41비트) + sequence(12비트)를 합쳐 53비트로 관리
     * 이 상태를 64비트 AtomicLong 한 개에 저장하고, CAS로 갱신한다.
     *
     * 구조 예:
     *  ---------------------------------------------------------
     *  | 22비트(미사용) | 41비트(timestamp) | 12비트(sequence)  |
     *  ---------------------------------------------------------
     *
     *  또는, 실사용은 timestamp를 상위로 두고 sequence를 하위로 둬서:
     *  ---------------------------------------------
     *  |  timestamp (상위 41비트) | sequence (12비트) |
     *  ---------------------------------------------
     */
    private val atomicState = AtomicLong(0L)

    /**
     * thread-safe하게 ID를 생성하는 핵심 로직
     */
    fun nextId(): Long {
        while (true) {
            val currentValue = atomicState.get()
            val lastTimestamp = currentValue ushr SEQUENCE_BITS // 상위 41비트
            val currentSequence = currentValue and MAX_SEQUENCE

            val now = currentTimeMillis() - epochStartMillis
            if (now < lastTimestamp) {
                // 시계가 크게 역행한 경우: 바로 예외를 던지거나
                // 혹은 작은 오프셋 정도면 대기나 보정할 수 있음.
                throw IllegalStateException("System clock moved backwards. Refusing to generate id for ${lastTimestamp - now}ms")
            }

            var nextTimestamp = lastTimestamp
            var nextSequence = currentSequence

            if (now == lastTimestamp) {
                // 같은 밀리초 -> sequence 증가
                nextSequence = (currentSequence + 1) and MAX_SEQUENCE
                if (nextSequence == 0L) {
                    // 해당 밀리초에서 시퀀스를 모두 소비하면 다음 밀리초까지 대기
                    val updatedMillis = waitNextMillis(lastTimestamp)
                    nextTimestamp = updatedMillis
                    nextSequence = 0L
                }
            } else {
                // 새 밀리초 -> 시퀀스 0으로 초기화
                nextTimestamp = now
                nextSequence = 0L
            }

            // 새롭게 갱신될 53비트 (상위 41: timestamp, 하위 12: sequence)
            val nextValue = (nextTimestamp shl SEQUENCE_BITS) or nextSequence

            // CAS 갱신 성공 시, 최종 ID 계산
            if (atomicState.compareAndSet(currentValue, nextValue)) {
                // 최종 ID의 비트 구조: 1bit(미사용) + 41bit(epoch) + 10bit(nodeId) + 12bit(sequence)
                // 여기서는 epoch = nextTimestamp
                // nodeId는 이미 0~(2^10-1) 범위
                // sequence = nextSequence
                return ((nextTimestamp shl (NODE_ID_BITS + SEQUENCE_BITS))   // timestamp를 왼쪽으로 22비트 이동
                        or (nodeId shl SEQUENCE_BITS)                         // nodeId를 왼쪽으로 12비트 이동
                        or (nextSequence and MAX_SEQUENCE))                   // sequence 12비트
            }
            // CAS 실패 시 다른 스레드가 먼저 갱신했으니 다시 루프
        }
    }

    /**
     * 시퀀스가 모두 소진되어, 다음 밀리초가 될 때까지 대기하는 메서드
     */
    private fun waitNextMillis(lastTimestamp: Long): Long {
        var ts = currentTimeMillis() - epochStartMillis
        while (ts <= lastTimestamp) {
            ts = currentTimeMillis() - epochStartMillis
        }
        return ts
    }

    /**
     * 별도 시간 소스가 필요하다면 주입할 수 있도록 메서드 추출
     */
    protected fun currentTimeMillis(): Long = System.currentTimeMillis()

    /**
     * 테스트 시계 역행 처리 등을 위해 임시 통계를 뽑아볼 수 있는 함수 예시
     */
    fun getStats(): Map<String, Any> {
        val st = atomicState.get()
        val timestampPart = st ushr SEQUENCE_BITS
        val sequencePart = st and MAX_SEQUENCE
        return mapOf(
            "lastTimestamp" to timestampPart,
            "sequence" to sequencePart,
            "nodeId" to nodeId
        )
    }
}
