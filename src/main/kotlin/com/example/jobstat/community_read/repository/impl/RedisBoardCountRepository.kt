package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.repository.BoardCountRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardCountRepository(
    private val redisTemplate: StringRedisTemplate
) : BoardCountRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    private companion object {
        const val BOARD_TOTAL_COUNT_KEY = "community-read::board-total-count"
        const val BOARD_COUNT_STATE_KEY = "community-read::board-count::state"
    }

    /**
     * total board count
     */
    override fun getTotalCount(): Long {
        return redisTemplate.opsForValue().get(BOARD_TOTAL_COUNT_KEY)?.toLong() ?: 0
    }

    override fun applyCountInPipeline(conn: StringRedisConnection, delta: Long) {
        val currentCountStr = conn.get(BOARD_TOTAL_COUNT_KEY) ?: "0"
        val currentCount = currentCountStr.toLong()
        val newCount = currentCount + delta
        conn.set(BOARD_TOTAL_COUNT_KEY, newCount.toString())
    }
}