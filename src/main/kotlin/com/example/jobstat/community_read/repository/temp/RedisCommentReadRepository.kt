package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.CommentReadModel
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisCommentReadRepository(
    private val redisTemplate: RedisTemplate<String, Any>
) : CommentReadRepository {

    companion object {
        private const val COMMENT_KEY_PREFIX = "comment:"
        private const val BOARD_COMMENTS_PREFIX = "board-comments:"
        private const val AUTHOR_COMMENTS_PREFIX = "author-comments:"
    }

    private val valueOps = redisTemplate.opsForValue()
    private val setOps = redisTemplate.opsForSet()
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun save(commentReadModel: CommentReadModel): CommentReadModel {
        try {
            val commentKey = getCommentKey(commentReadModel.id)
            val existing = valueOps.get(commentKey) as? CommentReadModel
            if (existing != null && existing.updatedAt >= commentReadModel.updatedAt) {
                log.info("CommentReadModel id {} is already up-to-date. Skipping save.", commentReadModel.id)
                return existing
            }
            valueOps.set(commentKey, commentReadModel)
            setOps.add(getBoardCommentsKey(commentReadModel.boardId), commentReadModel.id.toString())
            setOps.add(getAuthorCommentsKey(commentReadModel.author), commentReadModel.id.toString())
            return commentReadModel
        } catch (e: Exception) {
            log.error("댓글 저장 실패: commentId={}", commentReadModel.id, e)
            throw e
        }
    }

    override fun findById(commentId: Long): CommentReadModel? {
        try {
            val commentKey = getCommentKey(commentId)
            return valueOps.get(commentKey) as? CommentReadModel
        } catch (e: Exception) {
            log.error("댓글 조회 실패: commentId={}", commentId, e)
            throw e
        }
    }

    override fun findByBoardId(boardId: Long): List<CommentReadModel> {
        try {
            val commentIds = setOps.members(getBoardCommentsKey(boardId))
            return commentIds?.mapNotNull { commentId ->
                findById(commentId.toString().toLong())
            } ?: emptyList()
        } catch (e: Exception) {
            log.error("게시글별 댓글 조회 실패: boardId={}", boardId, e)
            throw e
        }
    }

    override fun findByAuthor(author: String): List<CommentReadModel> {
        try {
            val commentIds = setOps.members(getAuthorCommentsKey(author))
            return commentIds?.mapNotNull { commentId ->
                findById(commentId.toString().toLong())
            } ?: emptyList()
        } catch (e: Exception) {
            log.error("작성자별 댓글 조회 실패: author={}", author, e)
            throw e
        }
    }

    override fun delete(commentId: Long) {
        try {
            val commentKey = getCommentKey(commentId)
            val comment = findById(commentId)
            comment?.let {
                setOps.remove(getBoardCommentsKey(it.boardId), commentId.toString())
                setOps.remove(getAuthorCommentsKey(it.author), commentId.toString())
            }
            redisTemplate.delete(commentKey)
        } catch (e: Exception) {
            log.error("댓글 삭제 실패: commentId={}", commentId, e)
            throw e
        }
    }

    private fun getCommentKey(commentId: Long) = "$COMMENT_KEY_PREFIX$commentId"
    private fun getBoardCommentsKey(boardId: Long) = "$BOARD_COMMENTS_PREFIX$boardId"
    private fun getAuthorCommentsKey(author: String) = "$AUTHOR_COMMENTS_PREFIX$author"
}
