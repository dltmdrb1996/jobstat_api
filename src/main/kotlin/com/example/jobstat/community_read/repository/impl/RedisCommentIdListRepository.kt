package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.repository.CommentIdListRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository

@Repository
class RedisCommentIdListRepository(
    private val redisTemplate: StringRedisTemplate,
    private val cursorPaginationScript: RedisScript<List<*>>,
) : CommentIdListRepository {
    companion object {
        const val COMMENT_LIMIT_SIZE = 100L
        const val BOARD_COMMENTS_KEY_FORMAT = "community-read::board::%s::comment-list"

        fun getBoardCommentsKey(boardId: Long): String = BOARD_COMMENTS_KEY_FORMAT.format(boardId)
    }

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun readAllByBoard(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long> {
        try {
            val key = getBoardCommentsKey(boardId)
            val offset = pageable.offset
            val pageSize = pageable.pageSize.toLong()

            val totalSize = redisTemplate.opsForZSet().size(key) ?: 0

            val content =
                redisTemplate
                    .opsForZSet()
                    .reverseRange(key, offset, offset + pageSize - 1)
                    ?.map { it.toLong() } ?: emptyList()

            return PageImpl(content, pageable, totalSize)
        } catch (e: Exception) {
            log.error("게시글별 댓글 ID 리스트 조회 실패: boardId=$boardId, pageable=$pageable", e)
            throw e
        }
    }

    override fun readAllByBoardInfiniteScroll(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long> {
        try {
            val key = getBoardCommentsKey(boardId)
            return if (lastCommentId == null) {
                redisTemplate
                    .opsForZSet()
                    .reverseRange(key, 0, limit - 1)
                    ?.map { it.toLong() } ?: emptyList()
            } else {
                val lastCommentScore = redisTemplate.opsForZSet().score(key, toPaddedString(lastCommentId))
                if (lastCommentScore == null) {
                    emptyList()
                } else {
                    redisTemplate
                        .opsForZSet()
                        .reverseRangeByScore(key, 0.0, lastCommentScore - 0.000001, 0, limit)
                        ?.map { it.toLong() } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            log.error("무한 스크롤 게시글별 댓글 ID 리스트 조회 실패: boardId=$boardId, lastCommentId=$lastCommentId, limit=$limit", e)
            throw e
        }
    }

    override fun readCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long> {
        val key = getBoardCommentsKey(boardId)
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()

        val totalSize = redisTemplate.opsForZSet().size(key) ?: 0

        val content =
            redisTemplate
                .opsForZSet()
                .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
                ?.map { it.toLong() } ?: emptyList()

        return PageImpl(content, pageable, totalSize)
    }

    override fun readCommentsByBoardIdByCursor(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long> {
        val key = getBoardCommentsKey(boardId)

        if (lastCommentId == null) {
            return redisTemplate
                .opsForZSet()
                .reverseRange(key, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result =
            redisTemplate.execute(
                cursorPaginationScript,
                listOf(key),
                lastCommentId.toString(),
                limit.toString(),
            )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun getBoardCommentsKey(boardId: Long): String = BOARD_COMMENTS_KEY_FORMAT.format(boardId)

    fun getCommentCount(boardId: Long): Long {
        try {
            return redisTemplate.opsForZSet().size(getBoardCommentsKey(boardId)) ?: 0
        } catch (e: Exception) {
            log.error("게시글 댓글 수 조회 실패: boardId=$boardId", e)
            throw e
        }
    }

    override fun add(
        boardId: Long,
        commentId: Long,
        sortValue: Double,
    ) {
        try {
            val key = getBoardCommentsKey(boardId)
            val existingScore = redisTemplate.opsForZSet().score(key, toPaddedString(commentId))
            if (existingScore != null && existingScore == sortValue) {
                log.debug("댓글 ID ${commentId}는 이미 게시글 ${boardId}에 동일한 sortValue로 존재합니다.")
            } else {
                redisTemplate.opsForZSet().add(key, toPaddedString(commentId), sortValue)
            }
            val size = redisTemplate.opsForZSet().size(key) ?: 0
            if (size > COMMENT_LIMIT_SIZE) {
                redisTemplate.opsForZSet().removeRange(key, 0, size - COMMENT_LIMIT_SIZE - 1)
            }
        } catch (e: Exception) {
            log.error("게시글별 댓글 ID 리스트 추가 실패: boardId=$boardId, commentId=$commentId", e)
            throw e
        }
    }

    override fun delete(
        boardId: Long,
        commentId: Long,
    ) {
        try {
            redisTemplate.opsForZSet().remove(getBoardCommentsKey(boardId), toPaddedString(commentId))
        } catch (e: Exception) {
            log.error("게시글별 댓글 ID 리스트 삭제 실패: boardId=$boardId, commentId=$commentId", e)
            throw e
        }
    }

    override fun addCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
        score: Double,
    ) {
        val key = getBoardCommentsKey(boardId)
        conn.zAdd(key, score, commentId.toString())
        conn.zRemRange(key, 0, -(COMMENT_LIMIT_SIZE + 1))
    }

    override fun removeCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
    ) {
        val key = getBoardCommentsKey(boardId)
        conn.zRem(key, commentId.toString())
    }

    private fun toPaddedString(id: Long): String = "%019d".format(id)
}
