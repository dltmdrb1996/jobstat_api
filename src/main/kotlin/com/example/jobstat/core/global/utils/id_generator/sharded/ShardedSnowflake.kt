package com.example.jobstat.core.global.utils.id_generator.sharded

import com.example.jobstat.core.global.utils.id_generator.SnowflakeGenerator
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.log2

/**
 * 내부적으로 여러 SynchronizedSnowflakeCore 인스턴스(Shard)를 사용하고,
 * ID 구조에 Shard ID를 포함하여 중복을 방지하고 성능을 개선한 Snowflake 구현.
 * SnowflakeGenerator 인터페이스를 구현한다.
 */
class ShardedSnowflake(
    private val nodeId: Long = 0L,
    shardCount: Int = DEFAULT_SHARD_COUNT,
) : SnowflakeGenerator { // 인터페이스 구현

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val DEFAULT_SHARD_COUNT: Int = 16 // 기본 샤드 수 (2의 거듭제곱 권장)
        const val TOTAL_SEQUENCE_BITS = 12 // 노드 ID 뒤의 총 비트 수
        const val NODE_ID_BITS = 10 // 노드 ID 비트 수

        // 최대 노드 ID 계산
        const val MAX_NODE_ID = (1L shl NODE_ID_BITS) - 1

        // shardCount가 2의 거듭제곱인지 확인하고 비트 수 계산
        private fun calculateShardBits(count: Int): Int {
            require(count > 0 && (count and (count - 1) == 0)) {
                "shardCount must be positive power of 2 (e.g., 1, 2, 4, 8, 16...). Provided: $count"
            }
            return log2(count.toDouble()).toInt()
        }
    }

    private val shardCount: Int
    private val shardIdBits: Int
    private val coreSequenceBits: Int
    private val cores: Array<SynchronizedSnowflakeCore>
    private val counter = AtomicInteger(0)
    private val shardIndexMask: Int // 인덱스 계산용 마스크

    // 비트 쉬프트 상수 (동적으로 계산)
    private val nodeAndShardIdShift: Int
    private val shardIdShift: Int
    private val timestampShift: Int
    private val coreMaxSequence: Long

    init {
        require(nodeId in 0..MAX_NODE_ID) {
            "Node ID must be between 0 and $MAX_NODE_ID. Provided: $nodeId"
        }
        // shardCount 유효성 검사 및 비트 수 계산
        this.shardCount = shardCount
        this.shardIdBits = calculateShardBits(this.shardCount)
        this.coreSequenceBits = TOTAL_SEQUENCE_BITS - this.shardIdBits
        require(this.coreSequenceBits >= 0) {
            "Too many shards ($shardCount), not enough bits left for sequence. Max shards for 12 bits: ${1 shl TOTAL_SEQUENCE_BITS}"
        }
        this.shardIndexMask = this.shardCount - 1
        this.coreMaxSequence = (1L shl this.coreSequenceBits) - 1

        // 쉬프트 상수 계산
        this.shardIdShift = this.coreSequenceBits
        this.nodeAndShardIdShift = this.shardIdBits + this.coreSequenceBits
        this.timestampShift = NODE_ID_BITS + this.nodeAndShardIdShift

        // 각 코어 생성 시, 계산된 시퀀스 비트 수 전달
        cores =
            Array(this.shardCount) { index ->
                SynchronizedSnowflakeCore(nodeId, this.coreSequenceBits)
            }

        log.info(
            "Initialized ShardedSnowflake: NodeId={}, Shards={}, ShardBits={}, SequenceBitsPerCore={}",
            nodeId,
            this.shardCount,
            this.shardIdBits,
            this.coreSequenceBits,
        )
    }

    /**
     * 다음 고유 ID를 생성하여 반환한다.
     */
    override fun nextId(): Long {
        // 1. 다음 사용할 코어 인덱스(Shard ID) 결정 (Round-Robin)
        val shardIndex = counter.incrementAndGet() and shardIndexMask

        // 2. 선택된 코어에서 (timestamp | sequence) 부분 생성
        val timestampAndSequence = cores[shardIndex].nextTimestampAndSequence()

        // 3. 최종 ID 조립 (1(unused) + 41(ts) + 10(node) + shardIdBits + coreSequenceBits = 64)
        val timestamp = timestampAndSequence shr coreSequenceBits
        val sequence = timestampAndSequence and coreMaxSequence // Masking 필요

        // 비트 연산을 사용하여 최종 ID 조합
        return (
            (timestamp shl timestampShift) // 타임스탬프 부분
                or (nodeId shl nodeAndShardIdShift) // 노드 ID 부분
                or (shardIndex.toLong() shl shardIdShift) // 샤드 ID 부분
                or sequence
        ) // 시퀀스 부분
    }
}
