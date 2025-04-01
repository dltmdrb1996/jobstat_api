package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.core.constants.RedisKeyConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisCommentDetailRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper : ObjectMapper
) : CommentDetailRepository {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun create(commentModel: CommentReadModel, ttl: Duration) {
        try {
            val key = RedisKeyConstants.Comment.detailKey(commentModel.id)
            val existingJson = redisTemplate.opsForValue().get(key)
            if (existingJson != null) {
                val existing = objectMapper.readValue(existingJson, CommentReadModel::class.java)
                if (existing.updatedAt >= commentModel.updatedAt) {
                    log.info("Comment detail for id {} is up-to-date. Skipping creation.", commentModel.id)
                    return
                }
            }
            val json = objectMapper.writeValueAsString(commentModel)
            redisTemplate.opsForValue().set(key, json, ttl)
        } catch (e: Exception) {
            log.error("댓글 상세 정보 저장 실패: commentId={}", commentModel.id, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 상세 정보 저장 실패",
                detailInfo = "commentId: ${commentModel.id}"
            )
        }
    }

    override fun update(commentModel: CommentReadModel) {
        try {
            val key = RedisKeyConstants.Comment.detailKey(commentModel.id)
            val existingJson = redisTemplate.opsForValue().get(key)
            if (existingJson != null) {
                val existing = objectMapper.readValue(existingJson, CommentReadModel::class.java)
                if (existing.updatedAt >= commentModel.updatedAt) {
                    log.info("Comment detail for id {} is up-to-date. Skipping update.", commentModel.id)
                    return
                }
            }
            val json = objectMapper.writeValueAsString(commentModel)
            redisTemplate.opsForValue().set(key, json)
        } catch (e: Exception) {
            log.error("댓글 상세 정보 업데이트 실패: commentId={}", commentModel.id, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 상세 정보 업데이트 실패",
                detailInfo = "commentId: ${commentModel.id}"
            )
        }
    }

    override fun delete(commentId: Long) {
        try {
            redisTemplate.delete(RedisKeyConstants.Comment.detailKey(commentId))
        } catch (e: Exception) {
            log.error("댓글 상세 정보 삭제 실패: commentId={}", commentId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 상세 정보 삭제 실패",
                detailInfo = "commentId: $commentId"
            )
        }
    }

    override fun read(commentId: Long): CommentReadModel? {
        try {
            val key = RedisKeyConstants.Comment.detailKey(commentId)
            val json = redisTemplate.opsForValue().get(key) ?: return null
            return objectMapper.readValue(json, CommentReadModel::class.java)
        } catch (e: Exception) {
            log.error("댓글 상세 정보 조회 실패: commentId={}", commentId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 상세 정보 조회 실패",
                detailInfo = "commentId: $commentId"
            )
        }
    }

    override fun readAll(commentIds: List<Long>): Map<Long, CommentReadModel> {
        try {
            if (commentIds.isEmpty()) return emptyMap()
            val keys = commentIds.map { RedisKeyConstants.Comment.detailKey(it) }
            val values = redisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()
            return commentIds.zip(values)
                .filter { (_, json) -> json != null }
                .associate { (id, json) ->
                    id to objectMapper.readValue(json!!, CommentReadModel::class.java)
                }
        } catch (e: Exception) {
            log.error("댓글 상세 정보 일괄 조회 실패: commentIds={}", commentIds, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 상세 정보 일괄 조회 실패",
                detailInfo = "commentIds: $commentIds"
            )
        }
    }
}
