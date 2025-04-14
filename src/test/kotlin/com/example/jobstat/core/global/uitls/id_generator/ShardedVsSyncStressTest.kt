package com.example.jobstat.core.global.uitls.id_generator

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import com.example.jobstat.core.global.utils.id_generator.SynchronizedSnowflake as OptimizedSyncSnowflake
import com.example.jobstat.core.global.utils.id_generator.sharded.ShardedSnowflake as FixedShardedSnowflake

class ShardedVsSyncStressTest {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val NODE_ID = 1L
        private const val WARMUP_ITERATIONS = 50_000
        private const val TEST_DURATION_SECONDS = 15
        private const val SHARD_COUNT = 16
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 4, 8, 16, 32, 64, 128, 256])
    @DisplayName("최종 Snowflake 과부하 성능 비교 (Optimized Sync vs Fixed Sharded)")
    @Timeout(value = ((TEST_DURATION_SECONDS * 2) + 45).toLong(), unit = TimeUnit.SECONDS)
    fun compareFinalSafeImplementations_FixedDuration(threadCount: Int) {
        log.info(
            "\n===== 최종 과부하 성능 비교 테스트 시작 (Threads: {}, Duration: {}s) =====",
            threadCount,
            TEST_DURATION_SECONDS,
        )
        log.info("### Optimized Sync (단일 락, sleep) vs Sharded (다중 락, Shard ID 비트) ###")

        // 1. Optimized Synchronized 버전 테스트
        log.info("--- [Optimized Sync Generator (Single Lock, sleep wait)] 실행 ---")
        val optimizedSync = OptimizedSyncSnowflake(NODE_ID)
        val optimizedResult =
            runPerformanceTest_Simplified( // Simplified Helper 사용
                generatorName = "Optimized Sync",
                generator = { optimizedSync.nextId() },
                threadCount = threadCount,
                durationSeconds = TEST_DURATION_SECONDS,
                warmupIterations = WARMUP_ITERATIONS,
            )
        log.info("--- [Optimized Sync Generator] 완료 ---\n")
        assertEquals(0, optimizedResult.errors, "[Optimized Sync] 테스트 중 오류 발생!")

        Thread.sleep(1000)

        // 2. Fixed Sharded Snowflake 버전 테스트
        log.info("--- [Sharded Snowflake (Fixed, Shards: {})] 실행 ---", SHARD_COUNT)
        val sharded = FixedShardedSnowflake(NODE_ID, SHARD_COUNT)
        val shardedResult =
            runPerformanceTest_Simplified( // Simplified Helper 사용
                generatorName = "Sharded (Fixed, Shards: $SHARD_COUNT)",
                generator = { sharded.nextId() },
                threadCount = threadCount,
                durationSeconds = TEST_DURATION_SECONDS,
                warmupIterations = WARMUP_ITERATIONS,
            )
        log.info("--- [Sharded Snowflake (Fixed)] 완료 ---\n")
        assertEquals(0, shardedResult.errors, "[Sharded] 테스트 중 오류 발생!")

        // 결과 요약 로깅 (포맷 수정)
        log.info(
            "===== 최종 과부하 성능 비교 결과 요약 (Threads: {}, Duration: {}s) =====",
            threadCount,
            TEST_DURATION_SECONDS,
        )
        log.info(
            "[Optimized Sync  ] Total IDs: {}, Throughput: ${"%.2f".format(optimizedResult.throughput)} IDs/sec",
            optimizedResult.totalGeneratedIds,
            optimizedResult.throughput,
        )
        log.info(
            "[Sharded (Fixed) ] Total IDs: {}, Throughput: ${"%.2f".format(shardedResult.throughput)} IDs/sec",
            shardedResult.totalGeneratedIds,
            shardedResult.throughput,
        )
        log.info("======================================================================\n")
    }

    // 성능 테스트 결과 (Latency 제외)
    data class PerformanceResultSimplified(
        val generatorName: String,
        val threadCount: Int,
        val durationMillis: Long,
        val totalGeneratedIds: Long,
        val throughput: Double,
        val errors: Long,
    )

    /**
     * 고정 시간 동안 성능 테스트 (Latency 측정 제외 버전)
     */
    private fun runPerformanceTest_Simplified(
        generatorName: String,
        generator: () -> Long,
        threadCount: Int,
        durationSeconds: Int,
        warmupIterations: Int,
    ): PerformanceResultSimplified { // 반환 타입 변경

        val errors = AtomicLong(0)
        val totalGeneratedIds = AtomicLong(0)
        val testDurationMillis = durationSeconds * 1000L

        log.debug("[{}] Warmup 시작 ({} iterations)...", generatorName, warmupIterations)
        repeat(warmupIterations) {
            try {
                generator()
            } catch (e: Exception) {
                // 무시
            }
        }
        log.debug("[{}] Warmup 완료.", generatorName)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val startTimeMillis = System.currentTimeMillis()
        val endTimeMillis = startTimeMillis + testDurationMillis

        try {
            repeat(threadCount) {
                executor.submit {
                    var threadGeneratedCount = 0L
                    log.debug("[{}] Thread {} starting work...", generatorName, Thread.currentThread().id) // 스레드 시작 로그
                    try {
                        while (System.currentTimeMillis() < endTimeMillis) {
                            generator() // ID 생성만 반복
                            threadGeneratedCount++
                        }
                    } catch (e: Exception) {
                        log.error("[{}] ID 생성 중 예외 발생 (Thread: {})", generatorName, Thread.currentThread().id, e)
                        errors.incrementAndGet()
                    } finally {
                        totalGeneratedIds.addAndGet(threadGeneratedCount)
                        log.debug("[{}] Thread {} finished work. Count: {}", generatorName, Thread.currentThread().id, threadGeneratedCount) // 스레드 종료 로그
                        latch.countDown() // Latch 카운트 다운
                    }
                }
            }
            log.debug("[{}] All threads submitted. Awaiting latch...", generatorName)
            if (!latch.await(durationSeconds + 30L, TimeUnit.SECONDS)) { // Latch 대기 시간 더 늘림
                log.error("[{}] Latch await timed out! {} threads did not count down.", generatorName, latch.count)
                // Timeout 시에도 현재까지 집계된 결과로 진행
            } else {
                log.debug("[{}] Latch finished.", generatorName)
            }
        } finally {
            // Executor 종료 (이전과 동일)
            executor.shutdown()
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("[{}] Executor did not terminate after shutdown.", generatorName)
                    executor.shutdownNow()
                }
            } catch (ie: InterruptedException) {
                log.warn("[{}] Executor termination interrupted.", generatorName)
                executor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }

        val actualDurationMillis = System.currentTimeMillis() - startTimeMillis
        val finalTotalGenerated = totalGeneratedIds.get()
        val throughput =
            if (actualDurationMillis > 0 && finalTotalGenerated > 0) {
                finalTotalGenerated * 1000.0 / actualDurationMillis
            } else {
                0.0
            }

        // 최종 결과 로깅 (Latency 제외)
        log.info(
            """
            [{}] 결과:
            - 스레드 수: {}
            - 실제 실행 시간: {} ms
            - 총 생성 ID 수: {} (오류: {})
            - 처리율 (Throughput): ${"%.2f".format(throughput)} IDs/sec
            """.trimIndent(),
            generatorName,
            threadCount,
            actualDurationMillis,
            finalTotalGenerated,
            errors.get(),
        )

        return PerformanceResultSimplified( // 변경된 데이터 클래스 반환
            generatorName = generatorName,
            threadCount = threadCount,
            durationMillis = actualDurationMillis,
            totalGeneratedIds = finalTotalGenerated,
            throughput = throughput,
            errors = errors.get(),
        )
    }
}
