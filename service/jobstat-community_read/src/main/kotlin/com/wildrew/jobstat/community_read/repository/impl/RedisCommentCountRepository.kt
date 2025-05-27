package com.wildrew.jobstat.community_read.repository.impl

import com.wildrew.jobstat.community_read.repository.CommentCountRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisCommentCountRepository(
    private val redisTemplate: StringRedisTemplate,
) : CommentCountRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val COMMENT_TOTAL_COUNT_KEY = "community-read::comment-total-count"
        const val BOARD_COMMENT_COUNT_KEY_FORMAT = "community-read::board::%s::comment-count"
        const val COMMENT_COUNT_STATE_KEY = "community-read::comment-count::state"

        fun getBoardCommentCountKey(boardId: Long): String = BOARD_COMMENT_COUNT_KEY_FORMAT.format(boardId)

        fun getTotalCommentCountKey(): String = COMMENT_TOTAL_COUNT_KEY
    }

    override fun getCommentCountByBoardId(boardId: Long): Long {
        val key = getBoardCommentCountKey(boardId)
        return redisTemplate.opsForValue().get(key)?.toLong() ?: 0
    }

    override fun getTotalCount(): Long = redisTemplate.opsForValue().get(COMMENT_TOTAL_COUNT_KEY)?.toLong() ?: 0

    override fun applyBoardCommentCountInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        delta: Long,
    ) {
        val key = getBoardCommentCountKey(boardId)
        conn.incrBy(key, delta)
    }

    override fun applyTotalCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    ) {
        conn.incrBy(COMMENT_TOTAL_COUNT_KEY, delta)
    }
}
