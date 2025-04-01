package com.example.jobstat.community_read.repository

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisCommentIdListRepository(
    private val redisTemplate: StringRedisTemplate
) : CommentIdListRepository {

    companion object {
        const val BOARD_COMMENTS_KEY_FORMAT = "community-read::board::%s::comment-list"
        const val MAX_COMMENTS_PER_BOARD = 50
    }

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun add(boardId: Long, commentId: Long, sortValue: Double) {
        try {
            val key = getBoardCommentsKey(boardId)
            val existingScore = redisTemplate.opsForZSet().score(key, toPaddedString(commentId))
            if (existingScore != null && existingScore == sortValue) {
                log.info("Comment id {} already exists in board {} with same sortValue.", commentId, boardId)
            } else {
                redisTemplate.opsForZSet().add(key, toPaddedString(commentId), sortValue)
            }
            val size = redisTemplate.opsForZSet().size(key) ?: 0
            if (size > MAX_COMMENTS_PER_BOARD) {
                redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_COMMENTS_PER_BOARD - 1)
            }
        } catch (e: Exception) {
            log.error("게시글별 댓글 ID 리스트 추가 실패: boardId={}, commentId={}", boardId, commentId, e)
            throw e
        }
    }

    override fun delete(boardId: Long, commentId: Long) {
        try {
            redisTemplate.opsForZSet().remove(getBoardCommentsKey(boardId), toPaddedString(commentId))
        } catch (e: Exception) {
            log.error("게시글별 댓글 ID 리스트 삭제 실패: boardId={}, commentId={}", boardId, commentId, e)
            throw e
        }
    }

    override fun readAllByBoard(boardId: Long, offset: Long, limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(getBoardCommentsKey(boardId), offset, offset + limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("게시글별 댓글 ID 리스트 조회 실패: boardId={}, offset={}, limit={}", boardId, offset, limit, e)
            throw e
        }
    }

    override fun readAllByBoardInfiniteScroll(boardId: Long, lastCommentId: Long?, limit: Long): List<Long> {
        try {
            val key = getBoardCommentsKey(boardId)
            return if (lastCommentId == null) {
                redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1)
                    ?.map { it.toLong() } ?: emptyList()
            } else {
                val lastCommentScore = redisTemplate.opsForZSet().score(key, toPaddedString(lastCommentId))
                if (lastCommentScore == null) {
                    emptyList()
                } else {
                    redisTemplate.opsForZSet()
                        .reverseRangeByScore(key, 0.0, lastCommentScore - 0.000001, 0, limit)
                        ?.map { it.toLong() } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            log.error("무한 스크롤 게시글별 댓글 ID 리스트 조회 실패: boardId={}, lastCommentId={}, limit={}", boardId, lastCommentId, limit, e)
            throw e
        }
    }

    fun getCommentCount(boardId: Long): Long {
        try {
            return redisTemplate.opsForZSet().size(getBoardCommentsKey(boardId)) ?: 0
        } catch (e: Exception) {
            log.error("게시글 댓글 수 조회 실패: boardId={}", boardId, e)
            throw e
        }
    }

    private fun getBoardCommentsKey(boardId: Long): String {
        return BOARD_COMMENTS_KEY_FORMAT.format(boardId)
    }

    private fun toPaddedString(id: Long): String {
        return "%019d".format(id)
    }
}
