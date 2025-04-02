package com.example.jobstat.community_read.repository

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardCountRepository(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val BOARD_TOTAL_COUNT_KEY = "community-read::board-total-count"
        const val BOARD_COUNT_STATE_KEY = "community-read::board-count::state"
    }

    /**
     * total board count
     */
    fun getTotalCount(): Long {
        return redisTemplate.opsForValue().get(BOARD_TOTAL_COUNT_KEY)?.toLong() ?: 0
    }

    fun applyCountInPipeline(conn: StringRedisConnection, delta: Long, eventTs: Long) {
        val currentTsStr = conn.hGet(BOARD_COUNT_STATE_KEY, "lastCountUpdateTs") ?: "0"
        val currentTs = currentTsStr.toLongOrNull() ?: 0
        if (eventTs <= currentTs) {
            return
        }
        val currentCountStr = conn.get(BOARD_TOTAL_COUNT_KEY) ?: "0"
        val currentCount = currentCountStr.toLong()
        val newCount = currentCount + delta
        conn.set(BOARD_TOTAL_COUNT_KEY, newCount.toString())

        conn.hSet(BOARD_COUNT_STATE_KEY, "lastCountUpdateTs", eventTs.toString())
    }
}