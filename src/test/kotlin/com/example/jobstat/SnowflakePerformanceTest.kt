package com.example.jobstat

import com.example.jobstat.core.global.utils.Snowflake
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

class SnowflakeTest {

    private lateinit var snowflake: Snowflake

    @BeforeEach
    fun setup() {
        // 기본 구현체 (nodeId = 0)
        snowflake = Snowflake()
    }

    @Test
    @DisplayName("단일 스레드 - ID 순차 생성 & 유일성 검증")
    fun testSingleThreadUniquenessAndOrder() {
        val count = 100_000
        val generatedIds = ArrayList<Long>(count)

        repeat(count) {
            generatedIds.add(snowflake.nextId())
        }

        // 1) 중복 없는지 확인
        val distinctCount = generatedIds.distinct().size
        assertEquals(count, distinctCount, "단일 스레드에서 중복 ID가 발생했습니다.")

        // 2) 순차적 증가(이전 ID < 현재 ID) 확인
        for (i in 1 until count) {
            assertTrue(generatedIds[i] > generatedIds[i - 1], "단일 스레드: ID가 순차적으로 증가하지 않았습니다.")
        }
    }

    @Test
    @DisplayName("멀티 스레드 - ID 유일성 검증")
    fun testMultiThreadUniqueness() {
        val threadCount = 8
        val idsPerThread = 50_000
        val totalIds = threadCount * idsPerThread

        // 멀티스레드에서 생성할 ID 저장
        val allIds = ConcurrentHashMap<Long, Boolean>()

        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        for (i in 0 until threadCount) {
            executor.submit {
                repeat(idsPerThread) {
                    val id = snowflake.nextId()
                    allIds[id] = true
                }
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        assertEquals(
            totalIds,
            allIds.size,
            "멀티스레드 환경에서 생성된 ID에 중복이 발생했습니다."
        )
    }

    @Test
    @DisplayName("단일 스레드 성능 측정")
    fun testSingleThreadPerformance() {
        // JVM 워밍업을 위해 일정량 미리 생성
        repeat(100_000) {
            snowflake.nextId()
        }

        // 실제 측정
        val testIterations = 1_000_000
        val elapsedNs = measureNanoTime {
            repeat(testIterations) {
                snowflake.nextId()
            }
        }
        val elapsedMs = elapsedNs / 1_000_000
        val throughput = testIterations * 1_000_000_000L / elapsedNs

        println(
            """
            [단일 스레드 성능 테스트]
            - 생성 횟수: $testIterations
            - 소요 시간: ${elapsedMs} ms
            - 처리량: $throughput IDs/초
            """.trimIndent()
        )

        // 간단한 조건 정도만 점검 (테스트 실패가 아니고, 로그로 확인)
        assertTrue(throughput > 0, "처리량(throughput)이 비정상적으로 측정되었습니다.")
    }

    @Test
    @DisplayName("멀티 스레드 성능 측정")
    fun testMultiThreadPerformance() {
        // 멀티 스레드 환경에서의 처리량 비교
        val threadCount = 8
        val idsPerThread = 200_000
        val totalIds = threadCount * idsPerThread

        // 먼저 가볍게 워밍업
        repeat(100_000) {
            snowflake.nextId()
        }

        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        val startNs = System.nanoTime()
        for (i in 0 until threadCount) {
            executor.submit {
                repeat(idsPerThread) {
                    snowflake.nextId()
                }
                latch.countDown()
            }
        }

        latch.await()
        val elapsedNs = System.nanoTime() - startNs
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        val elapsedMs = elapsedNs / 1_000_000
        val throughput = totalIds * 1_000_000_000L / elapsedNs

        println(
            """
            [멀티 스레드 성능 테스트]
            - 스레드: $threadCount
            - 생성 횟수 (총): $totalIds
            - 소요 시간: ${elapsedMs} ms
            - 처리량: $throughput IDs/초
            """.trimIndent()
        )

        assertTrue(throughput > 0, "멀티 스레드 처리량이 비정상적으로 측정되었습니다.")
    }

    @Test
    @DisplayName("시계 역행 처리 테스트(Reflection 기반)")
    fun testClockBackwards() {
        // 1) Snowflake 인스턴스 생성
        val snowflake = Snowflake(nodeId = 1L)

        // 2) 먼저 정상적으로 ID 하나 발급 (워밍업 개념)
        val normalId = snowflake.nextId()
        assertTrue(normalId > 0, "첫 번째 ID가 정상 생성되지 않았습니다.")

        // 3) Reflection으로 atomicState 필드에 접근하여 현재 상태 가져오기
        val atomicStateField = Snowflake::class.java.getDeclaredField("atomicState").apply {
            isAccessible = true
        }
        val atomicLong = atomicStateField.get(snowflake) as AtomicLong

        // 4) Snowflake가 내부적으로 사용하는 epochStartMillis를 확인 (Reflection으로 가져오기)
        val epochStartMillisField = Snowflake::class.java.getDeclaredField("epochStartMillis").apply {
            isAccessible = true
        }
        val epochStartMillis = epochStartMillisField.getLong(snowflake)

        // 5) 현재 시각 기준으로 한참 미래(예: +10초)로 lastTimestamp를 세팅
        //    => nextId() 실행 시, 실제 System.currentTimeMillis()가 이를 따라잡지 못해 'clock moved backwards'가 유발됨
        val futureOffsetMillis = 10_000L // 10초
        val futureTimestamp = (System.currentTimeMillis() + futureOffsetMillis) - epochStartMillis

        // 6) Snowflake에서는 atomicState = (timestamp << 12) | sequence
        //    일단 sequence(12비트)는 0으로 두고, timestamp만 미래 값으로
        val newAtomicValue = (futureTimestamp shl 12) // sequence = 0
        atomicLong.set(newAtomicValue)

        // 7) 이제 nextId() 호출 -> 내부 로직에서 now < lastTimestamp 조건이 발생하며 예외가 터져야 함
        assertThrows(IllegalStateException::class.java) {
            snowflake.nextId()
        }.also {
            println("시계 역행 예외가 정상적으로 발생했습니다: $it")
        }
    }
}
