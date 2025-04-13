package com.example.jobstat.core.global.uitls.id_generator // 테스트 대상 클래스와 동일 패키지 가정

// 테스트 대상 클래스 import (경로는 실제 프로젝트에 맞게 조정)
import com.example.jobstat.core.global.utils.id_generator.sharded.ShardedSnowflake.Companion.NODE_ID_BITS
import com.example.jobstat.core.global.utils.id_generator.sharded.SynchronizedSnowflakeCore
import com.example.jobstat.core.global.utils.id_generator.sharded.ShardedSnowflake as FixedShardedSnowflake
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class FixedShardedSnowflakeTest {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEFAULT_NODE_ID = 1L
        private const val DEFAULT_SHARD_COUNT = 16 // 테스트 시 사용할 기본 샤드 수

        // Reflection용 상수
        private const val CORE_LAST_TIMESTAMP_FIELD_NAME = "lastTimestamp" // Synchronized 버전의 필드 이름
        private const val CORE_EPOCH_MILLIS_FIELD_NAME = "epochMillis"    // Synchronized 버전의 필드 이름 (epochStartMillis -> epochMillis로 변경되었을 수 있으니 확인)
                                                                          // -> 이전 코드 확인 결과 epochMillis 가 맞음.
        // Shard ID 비트 계산 (ShardedSnowflake 내부 로직과 일치해야 함)
        private val SHARD_ID_BITS = FixedShardedSnowflake.NODE_ID_BITS // companion object 접근
        private val CORE_SEQUENCE_BITS = FixedShardedSnowflake.TOTAL_SEQUENCE_BITS
    }

    // --- ID 정합성(유일성) 테스트 ---
    // 목적: 수정된 ShardedSnowflake가 높은 부하에서도 ID 중복을 발생시키지 않는지 확인
    // 예상 결과: 성공 (오류/중복 0건)
    @ParameterizedTest
    @ValueSource(ints = [1, 8, 64, 128, 256]) // 스레드 수
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

        log.info("[Fixed Sharded 유일성 - {} 스레드, {} 샤드] 테스트 시작 (ID/스레드: {}, 총 예상 ID: {})",
            threadCount, shardCount, idsPerThread, totalIdsExpected)

        try {
            runConcurrentTest(threadCount) { latch ->
                try {
                    repeat(idsPerThread) {
                        val id = shardedSnowflake.nextId() // 수정된 ShardedSnowflake 사용
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
        log.info("[Fixed Sharded 유일성 - {} 스레드, {} 샤드] 테스트 통과 (생성된 ID: {})",
            threadCount, shardCount, allIds.size)
    }


    // --- 시간 역행 테스트 ---
    // 목적: 내부 코어의 시계가 역행하는 상황을 시뮬레이션했을 때,
    //      Synchronized Core의 max() 처리 로직 덕분에 예외가 발생하지 않고 정상 처리되는지 확인
    // 예상 결과: 성공 (예외 발생 없음)
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
        log.info("[Fixed Sharded 시간 역행] 내부 Core (인덱스 0, Hash: {}) 조작 시작...", targetCore.hashCode())

        // 3. 선택된 Core의 내부 상태 조작 (lastTimestamp 필드 사용)
        // KMutableProperty1 사용 시 Kotlin Reflection 의존성 필요할 수 있음
        // 여기서는 Java Reflection 방식으로 필드 직접 접근 시도 (Kotlin 컴파일러가 backing field 생성)
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
        log.info("[Fixed Sharded 시간 역행] 내부 Core의 lastTimestamp 강제 변경: {} -> {}", previousTimestamp, futureTimestamp)

        // 4. ShardedSnowflake.nextId() 반복 호출하여 예외가 발생하지 않는지 확인
        // Round-robin으로 분배되므로, 최대 shardCount + alpha 만큼 호출
        val maxAttempts = shardCount * 2
        assertDoesNotThrow(
            "내부 Core의 시계 역행 상황(lastTimestamp 조작) 시 예외가 발생해서는 안 됩니다 (max() 처리 기대)."
        ) {
            for (i in 1..maxAttempts) {
                try {
                    val id = shardedSnowflake.nextId()
                    val generatedTimestamp = (id shr (NODE_ID_BITS + SHARD_ID_BITS + CORE_SEQUENCE_BITS)) // 최종 ID에서 타임스탬프 추출
                    log.debug("[Fixed Sharded 시간 역행] 시도 {}: ID {} 생성됨 (Timestamp: {})", i, id, generatedTimestamp)
                    // 추가 검증: 생성된 ID의 타임스탬프가 조작된 futureTimestamp 근처인지 확인 가능 (선택 사항)
                    // if (i <= shardCount && generatedTimestamp < futureTimestamp - 1) {
                    //     log.warn("Generated timestamp {} is unexpectedly lower than future timestamp {}", generatedTimestamp, futureTimestamp)
                    // }
                } catch (e: Exception) {
                    // 어떤 예외든 발생하면 테스트 실패
                     log.error("[Fixed Sharded 시간 역행] 시도 {} 중 예상치 못한 예외 발생!", i, e)
                    throw e // assertDoesNotThrow 가 잡도록 예외 다시 던짐
                }
            }
        }

        log.info("[Fixed Sharded 시간 역행] 예외 없이 {}회 ID 생성 완료.", maxAttempts)

        // (선택) 조작된 상태 원상 복구 시도
        try {
            lastTimestampField.setLong(targetCore, previousTimestamp)
            log.info("[Fixed Sharded 시간 역행] 내부 Core의 lastTimestamp 원상 복구 시도 완료.")
        } catch (e: Exception) {
            log.warn("내부 Core 상태 복구 중 오류 발생", e)
        }
    }


    // --- Helper Methods ---
    private fun runConcurrentTest(threadCount: Int, task: (CountDownLatch) -> Unit) {
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