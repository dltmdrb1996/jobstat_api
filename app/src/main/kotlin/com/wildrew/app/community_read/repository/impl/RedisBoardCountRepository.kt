package com.wildrew.app.community_read.repository.impl

import com.wildrew.app.community_read.repository.BoardCountRepository
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

    override fun getTotalCount(): Long = redisTemplate.opsForValue().get(BOARD_TOTAL_COUNT_KEY)?.toLongOrNull() ?: 0L

    override fun applyCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    ) {
        conn.incrBy(BOARD_TOTAL_COUNT_KEY, delta)
    }
}
