package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.repository.BoardCountRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardCountRepository(
    private val redisTemplate: StringRedisTemplate,
) : BoardCountRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val BOARD_TOTAL_COUNT_KEY = "community-read::board-total-count"
        const val BOARD_COUNT_STATE_KEY = "community-read::board-count::state"
    }

    // 조회 관련 메소드

    /**
     * 게시글 총 개수 조회
     */
    override fun getTotalCount(): Long = redisTemplate.opsForValue().get(BOARD_TOTAL_COUNT_KEY)?.toLong() ?: 0

    // 수정 관련 메소드 (파이프라인)

    /**
     * 파이프라인을 통한 게시글 개수 증감
     */
    override fun applyCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    ) {
        val currentCountStr = conn.get(BOARD_TOTAL_COUNT_KEY) ?: "0"
        val currentCount = currentCountStr.toLong()
        val newCount = currentCount + delta
        conn.set(BOARD_TOTAL_COUNT_KEY, newCount.toString())
    }
}
