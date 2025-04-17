package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.repository.impl.RedisCommentCountRepository
import com.example.jobstat.utils.base.RedisIntegrationTestSupport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.StringRedisConnection

@DisplayName("RedisCommentCountRepository 통합 테스트")
class RedisCommentCountRepositoryIntegrationTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var commentCountRepository: RedisCommentCountRepository

    private val boardId1 = 1L
    private val boardId2 = 2L
    private val board1CountKey = RedisCommentCountRepository.getBoardCommentCountKey(boardId1)
    private val board2CountKey = RedisCommentCountRepository.getBoardCommentCountKey(boardId2)
    private val totalCountKey = RedisCommentCountRepository.getTotalCommentCountKey()

    @BeforeEach fun setUp() {
        flushAll()
    }

    @Nested
    @DisplayName("getCommentCountByBoardId 메서드")
    inner class GetCommentCountByBoardId {
        @Test
        @DisplayName("성공: 키가 없을 때 0을 반환한다")
        fun `getCommentCountByBoardId returns 0 when key missing`() {
            assertEquals(0L, commentCountRepository.getCommentCountByBoardId(boardId1))
        }

        @Test
        @DisplayName("성공: 키가 존재할 때 저장된 값을 Long으로 반환한다")
        fun `getCommentCountByBoardId returns stored value`() {
            redisTemplate.opsForValue().set(board1CountKey, "55")
            assertEquals(55L, commentCountRepository.getCommentCountByBoardId(boardId1))
        }
    }

    @Nested
    @DisplayName("getTotalCount 메서드")
    inner class GetTotalCount {
        @Test
        @DisplayName("성공: 키가 없을 때 0을 반환한다")
        fun `getTotalCount returns 0 when key missing`() {
            assertEquals(0L, commentCountRepository.getTotalCount())
        }

        @Test
        @DisplayName("성공: 키가 존재할 때 저장된 값을 Long으로 반환한다")
        fun `getTotalCount returns stored value`() {
            redisTemplate.opsForValue().set(totalCountKey, "12345")
            assertEquals(12345L, commentCountRepository.getTotalCount())
        }
    }

    @Nested
    @DisplayName("applyBoardCommentCountInPipeline 메서드")
    inner class ApplyBoardCommentCountInPipeline {
        @Test
        @DisplayName("성공: 파이프라인 내에서 delta만큼 특정 게시글 댓글 수 증감 (음수 허용)")
        fun `applyBoardCommentCountInPipeline increments decrements allowing negative`() {
            redisTemplate.opsForValue().set(board1CountKey, "5")
            redisTemplate.opsForValue().set(board2CountKey, "10") // Given
            redisTemplate.executePipelined { connection ->
                // When
                val sConn = connection as StringRedisConnection
                commentCountRepository.applyBoardCommentCountInPipeline(sConn, boardId1, 3L)
                commentCountRepository.applyBoardCommentCountInPipeline(sConn, boardId1, -1L)
                commentCountRepository.applyBoardCommentCountInPipeline(sConn, boardId2, -12L)
                null
            } // Then
            assertEquals(7L, commentCountRepository.getCommentCountByBoardId(boardId1)) // 5 + 3 - 1 = 7
            assertEquals(-2L, commentCountRepository.getCommentCountByBoardId(boardId2), "INCRBY는 음수를 허용해야 함")
            assertEquals("-2", redisTemplate.opsForValue().get(board2CountKey), "Redis 값은 음수 문자열이어야 함")
        }
    }

    @Nested
    @DisplayName("applyTotalCountInPipeline 메서드")
    inner class ApplyTotalCountInPipeline {
        @Test
        @DisplayName("성공: 파이프라인 내에서 delta만큼 전체 댓글 수 증감 (음수 허용)")
        fun `applyTotalCountInPipeline increments decrements allowing negative`() {
            redisTemplate.opsForValue().set(totalCountKey, "100") // Given
            redisTemplate.executePipelined { connection ->
                // When
                val sConn = connection as StringRedisConnection
                commentCountRepository.applyTotalCountInPipeline(sConn, 20L)
                commentCountRepository.applyTotalCountInPipeline(sConn, -5L)
                commentCountRepository.applyTotalCountInPipeline(sConn, -150L)
                null
            } // Then
            assertEquals(-35L, commentCountRepository.getTotalCount(), "INCRBY는 음수를 허용해야 함") // 100 + 20 - 5 - 150 = -35
            assertEquals("-35", redisTemplate.opsForValue().get(totalCountKey), "Redis 값은 음수 문자열이어야 함")
        }
    }
}
