package com.wildrew.jobstat.community_read.repository.impl

import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.community_read.repository.CommentDetailRepository
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisCommentDetailRepository(
    private val redisTemplate: StringRedisTemplate,
    private val dataSerializer: DataSerializer,
) : CommentDetailRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun detailKey(commentId: Long) = "comment:detail:json::$commentId"

        fun detailStateKey(commentId: Long) = "comment:detailstate::$commentId"
    }

    override fun findCommentDetail(commentId: Long): CommentReadModel? =
        redisTemplate
            .opsForValue()
            .get(detailKey(commentId))
            ?.let { dataSerializer.deserialize(it, CommentReadModel::class) }

    override fun findCommentDetails(commentIds: List<Long>): Map<Long, CommentReadModel> {
        if (commentIds.isEmpty()) return emptyMap()

        val keys = commentIds.map { detailKey(it) }
        val values = redisTemplate.opsForValue().multiGet(keys)
        val resultMap = mutableMapOf<Long, CommentReadModel>()
        commentIds.forEachIndexed { index, commentId ->
            val data = values?.get(index)
            if (data != null) {
                dataSerializer.deserialize(data, CommentReadModel::class)?.let {
                    resultMap[commentId] = it
                }
            }
        }
        return resultMap
    }

    override fun saveCommentDetail(
        comment: CommentReadModel,
        eventTs: Long,
    ) {
        val json =
            dataSerializer.serialize(comment)
                ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        redisTemplate.opsForValue().set(detailKey(comment.id), json)
        log.info("댓글 상세 정보 저장/업데이트 (SET): commentId=${comment.id}")
    }

    override fun saveCommentDetails(
        comments: List<CommentReadModel>,
        eventTs: Long,
    ) {
        if (comments.isEmpty()) return
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            comments.forEach { c ->
                saveCommentDetailInPipeline(stringConn, c)
            }
            null
        }
    }

    override fun saveCommentDetailInPipeline(
        conn: StringRedisConnection,
        comment: CommentReadModel,
    ) {
        val json =
            dataSerializer.serialize(comment)
                ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        conn.set(detailKey(comment.id), json)
    }
}
