package com.wildrew.app.community_read.repository

import com.wildrew.app.community_read.repository.impl.RedisBoardCountRepository
import com.wildrew.app.utils.base.RedisIntegrationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.StringRedisConnection

@DisplayName("RedisBoardCountRepository 통합 테스트")
class RedisBoardCountRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var boardCountRepository: RedisBoardCountRepository

    private val totalCountKey = RedisBoardCountRepository.BOARD_TOTAL_COUNT_KEY

    @BeforeEach
    fun setUp() {
        flushAll()
    }

    @Nested
    @DisplayName("getTotalCount 메서드")
    inner class GetTotalCount {
        @Test
        @DisplayName("성공: 키가 없을 때 0을 반환한다")
        fun `given key does not exist, when getTotalCount, then return 0`() {
            // When
            val count = boardCountRepository.getTotalCount()
            // Then
            assertEquals(0L, count)
        }

        @Test
        @DisplayName("성공: 키가 존재할 때 저장된 값을 Long으로 반환한다")
        fun `given key exists, when getTotalCount, then return stored value as Long`() {
            // Given
            val expectedCount = 123L
            redisTemplate.opsForValue().set(totalCountKey, expectedCount.toString())
            // When
            val count = boardCountRepository.getTotalCount()
            // Then
            assertEquals(expectedCount, count)
        }

        @Test
        @DisplayName("성공: 저장된 값이 숫자가 아닐 경우 0을 반환 (기본값 처리)")
        fun `given non-numeric value exists, when getTotalCount, then return 0`() {
            // Given
            redisTemplate.opsForValue().set(totalCountKey, "not_a_number")
            // When
            val count = boardCountRepository.getTotalCount()
            // Then
            assertEquals(0L, count)
        }
    }

    @Nested
    @DisplayName("applyCountInPipeline 메서드")
    inner class ApplyCountInPipeline {
        @Test
        @DisplayName("성공: 파이프라인 내에서 delta만큼 전체 게시글 수를 증가시킨다")
        fun `given delta, when applyCountInPipeline, then increment total count correctly`() {
            // Given
            val initialCount = 10L
            val delta1 = 5L
            val delta2 = -3L
            redisTemplate.opsForValue().set(totalCountKey, initialCount.toString())
            // When
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                boardCountRepository.applyCountInPipeline(stringConn, delta1)
                boardCountRepository.applyCountInPipeline(stringConn, delta2)
                null
            }
            // Then
            val finalCount = boardCountRepository.getTotalCount()
            assertEquals(initialCount + delta1 + delta2, finalCount)
        }

        @Test
        @DisplayName("성공: 키가 없을 때 0에서 시작하여 delta를 적용한다")
        fun `given no initial key, when applyCountInPipeline, then apply delta starting from 0`() {
            // Given
            val delta = 7L
            // When
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                boardCountRepository.applyCountInPipeline(stringConn, delta)
                null
            }
            // Then
            val finalCount = boardCountRepository.getTotalCount()
            assertEquals(delta, finalCount)
        }

        @Test
        @DisplayName("성공: 감소 결과가 음수여도 Redis에는 음수 문자열이 저장된다")
        fun `given delta makes count negative, when applyCountInPipeline, then stores negative string`() {
            // Given
            val delta = -5L

            // When
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                boardCountRepository.applyCountInPipeline(stringConn, delta)
                null
            }
            // Then
            val rawValue = redisTemplate.opsForValue().get(totalCountKey)
            assertEquals("-5", rawValue)
            assertEquals(-5L, boardCountRepository.getTotalCount())
        }
    }
}
