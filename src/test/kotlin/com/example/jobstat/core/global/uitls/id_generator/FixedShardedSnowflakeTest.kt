package com.example.jobstat.core.global.uitls.id_generator

import com.example.jobstat.core.global.utils.id_generator.sharded.ShardedSnowflake.Companion.NODE_ID_BITS
import com.example.jobstat.core.global.utils.id_generator.sharded.SynchronizedSnowflakeCore
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import com.example.jobstat.core.global.utils.id_generator.sharded.ShardedSnowflake as FixedShardedSnowflake

class FixedShardedSnowflakeTest {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEFAULT_NODE_ID = 1L
        private const val DEFAULT_SHARD_COUNT = 16 // 테스트 시 사용할 기본 샤드 수

        private const val CORE_LAST_TIMESTAMP_FIELD_NAME = "lastTimestamp"
        private const val CORE_EPOCH_MILLIS_FIELD_NAME = "epochMillis"

        private val SHARD_ID_BITS = FixedShardedSnowflake.NODE_ID_BITS
        private val CORE_SEQUENCE_BITS = FixedShardedSnowflake.TOTAL_SEQUENCE_BITS
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 8, 64, 128, 256])
    @DisplayName("[Fixed Sharded] 멀티 스레드 - ID 유일성 검증")
    @Timeout(value = 60, unit = TimeUnit.SECONDS) // 시간 충분히 부여
    fun testFixedSharded_MultiThreadUniqueness(threadCount: Int) {
        val shardCount = DEFAULT_SHARD_COUNT
        val idsPerThread = 200_000 / threadCount + 1 // 이전보다 약간 늘림
        val totalIdsExpected = threadCount * idsPerThread.toLong()
        val allIds = ConcurrentHashMap<Long, Boolean>()
        // 수정된 ShardedSnowflake 인스턴스 생성
        val shardedSnowflake = FixedShardedSnowflake(nodeId = DEFAULT_NODE_ID, shardCount = shardCount)
        val errors = AtomicLong(0)

        log.debug(
            "[Fixed Sharded 유일성 - {} 스레드, {} 샤드] 테스트 시작 (ID/스레드: {}, 총 예상 ID: {})",
            threadCount,
            shardCount,
            idsPerThread,
            totalIdsExpected,
        )

        try {
            runConcurrentTest(threadCount) { latch ->
                try {
                    repeat(idsPerThread) {
                        val id = shardedSnowflake.nextId()
                        if (allIds.put(id, true) != null) {
                            // 중복 발생 시 즉시 테스트 실패 처리도 고려 가능
                            log.error("##### [Fixed Sharded 유일성] 중복 ID 감지됨!: {}", id)
                            errors.incrementAndGet()
                        }
                    }
                } catch (e: Exception) {
                    log.error("[Fixed Sharded 유일성] ID 생성 중 예외 발생", e)
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        } catch (e: TimeoutException) {
            log.error("[Fixed Sharded 유일성 - {} 스레드] 테스트 시간 초과!", threadCount, e)
            fail("테스트 시간 초과") // 시간 초과 시 실패 처리
        }

        // 최종 검증: 오류가 없고, 생성된 고유 ID 수가 예상과 정확히 일치해야 함
        assertEquals(0, errors.get(), "[Fixed Sharded 유일성] ID 생성 중 오류 또는 중복 발생")
        assertEquals(totalIdsExpected, allIds.size.toLong(), "[Fixed Sharded 유일성] 생성된 ID 개수가 예상과 다릅니다 (중복 발생 가능성).")
        log.debug(
            "[Fixed Sharded 유일성 - {} 스레드, {} 샤드] 테스트 통과 (생성된 ID: {})",
            threadCount,
            shardCount,
            allIds.size,
        )
    }

    @Test
    @DisplayName("[Fixed Sharded] 시간 역행 처리 테스트 (내부 Core 조작, 예외 미발생 검증)")
    fun testFixedSharded_ClockBackwards_NoException() {
        val shardCount = DEFAULT_SHARD_COUNT
        val nodeId = DEFAULT_NODE_ID
        val shardedSnowflake = FixedShardedSnowflake(nodeId = nodeId, shardCount = shardCount)

        // 1. 내부 cores 배열 가져오기 (Reflection)
        val coresField = FixedShardedSnowflake::class.declaredMemberProperties.firstOrNull { it.name == "cores" }?.apply { isAccessible = true }
        assertNotNull(coresField, "내부 필드 'cores'를 찾을 수 없습니다.")
        val cores = coresField!!.get(shardedSnowflake) as Array<SynchronizedSnowflakeCore>
        assertTrue(cores.isNotEmpty(), "내부 SnowflakeCore 배열이 비어있습니다.")

        // 2. 조작할 내부 Core 선택 (예: 첫 번째 코어)
        val targetCore = cores[0]
        log.debug("[Fixed Sharded 시간 역행] 내부 Core (인덱스 0, Hash: {}) 조작 시작...", targetCore.hashCode())

        // 3. 선택된 Core의 내부 상태 조작 (lastTimestamp 필드 사용)
        val lastTimestampField = SynchronizedSnowflakeCore::class.java.getDeclaredField(CORE_LAST_TIMESTAMP_FIELD_NAME).apply { isAccessible = true }
        val epochMillisField = SynchronizedSnowflakeCore::class.java.getDeclaredField(CORE_EPOCH_MILLIS_FIELD_NAME).apply { isAccessible = true }

        assertNotNull(lastTimestampField, "내부 필드 '$CORE_LAST_TIMESTAMP_FIELD_NAME'를 찾을 수 없습니다.")
        assertNotNull(epochMillisField, "내부 필드 '$CORE_EPOCH_MILLIS_FIELD_NAME'를 찾을 수 없습니다.")

        val epochMillis = epochMillisField.getLong(targetCore)

        // 현재 시간보다 미래의 타임스탬프 강제 설정
        val futureOffsetMillis = 10_000L
        val futureTimestamp = (System.currentTimeMillis() + futureOffsetMillis) - epochMillis

        val previousTimestamp = lastTimestampField.getLong(targetCore)
        lastTimestampField.setLong(targetCore, futureTimestamp) // lastTimestamp 값을 미래로 설정
        log.debug("[Fixed Sharded 시간 역행] 내부 Core의 lastTimestamp 강제 변경: {} -> {}", previousTimestamp, futureTimestamp)

        // 4. ShardedSnowflake.nextId() 반복 호출하여 예외가 발생하지 않는지 확인
        val maxAttempts = shardCount * 2
        assertDoesNotThrow(
            "내부 Core의 시계 역행 상황(lastTimestamp 조작) 시 예외가 발생해서는 안 됩니다 (max() 처리 기대).",
        ) {
            for (i in 1..maxAttempts) {
                try {
                    val id = shardedSnowflake.nextId()
                    val generatedTimestamp = (id shr (NODE_ID_BITS + SHARD_ID_BITS + CORE_SEQUENCE_BITS)) // 최종 ID에서 타임스탬프 추출
                    log.debug("[Fixed Sharded 시간 역행] 시도 {}: ID {} 생성됨 (Timestamp: {})", i, id, generatedTimestamp)
                } catch (e: Exception) {
                    log.error("[Fixed Sharded 시간 역행] 시도 {} 중 예상치 못한 예외 발생!", i, e)
                    throw e
                }
            }
        }

        log.debug("[Fixed Sharded 시간 역행] 예외 없이 {}회 ID 생성 완료.", maxAttempts)

        try {
            lastTimestampField.setLong(targetCore, previousTimestamp)
            log.debug("[Fixed Sharded 시간 역행] 내부 Core의 lastTimestamp 원상 복구 시도 완료.")
        } catch (e: Exception) {
            log.warn("내부 Core 상태 복구 중 오류 발생", e)
        }
    }

    private fun runConcurrentTest(
        threadCount: Int,
        task: (CountDownLatch) -> Unit,
    ) {
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        try {
            repeat(threadCount) {
                executor.submit { task(latch) }
            }
            if (!latch.await(1, TimeUnit.MINUTES)) { // 시간 제한
                log.error("Concurrent test timed out waiting for threads!")
                throw TimeoutException("Test timed out for threadCount=$threadCount")
            }
        } finally {
            executor.shutdown()
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate gracefully.")
                executor.shutdownNow()
            }
        }
    }
}
