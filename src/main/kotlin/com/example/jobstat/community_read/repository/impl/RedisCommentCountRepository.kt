package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.repository.CommentCountRepository
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

        fun getBoardCommentCountKey(boardId: Long): String =
            BOARD_COMMENT_COUNT_KEY_FORMAT.format(boardId)

        fun getTotalCommentCountKey(): String =
            COMMENT_TOTAL_COUNT_KEY
    }

    // 조회 관련 메소드

    /**
     * 게시글별 댓글 총 개수 조회
     */
    override fun getCommentCountByBoardId(boardId: Long): Long {
        val key = BOARD_COMMENT_COUNT_KEY_FORMAT.format(boardId)
        return redisTemplate.opsForValue().get(key)?.toLong() ?: 0
    }

    /**
     * 전체 댓글 개수 조회
     */
    override fun getTotalCount(): Long = redisTemplate.opsForValue().get(COMMENT_TOTAL_COUNT_KEY)?.toLong() ?: 0

    // 수정 관련 메소드 (파이프라인)

    /**
     * 파이프라인에서 게시글별 댓글 카운트 증감
     */
    override fun applyBoardCommentCountInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        delta: Long,
    ) {
        val key = BOARD_COMMENT_COUNT_KEY_FORMAT.format(boardId)
        val currentCountStr = conn.get(key) ?: "0"
        val currentCount = currentCountStr.toLong()
        val newCount = currentCount + delta
        if (newCount < 0) {
            log.warn("게시글 댓글 수가 음수가 되므로 0으로 설정합니다: boardId=$boardId, currentCount=$currentCount, delta=$delta")
            conn.set(key, "0")
        } else {
            conn.set(key, newCount.toString())
        }
    }

    /**
     * 파이프라인에서 전체 댓글 카운트 증감
     */
    override fun applyTotalCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    ) {
        val currentCountStr = conn.get(COMMENT_TOTAL_COUNT_KEY) ?: "0"
        val currentCount = currentCountStr.toLong()
        val newCount = currentCount + delta
        if (newCount < 0) {
            log.warn("전체 댓글 수가 음수가 되므로 0으로 설정합니다: currentCount=$currentCount, delta=$delta")
            conn.set(COMMENT_TOTAL_COUNT_KEY, "0")
        } else {
            conn.set(COMMENT_TOTAL_COUNT_KEY, newCount.toString())
        }
    }
}
