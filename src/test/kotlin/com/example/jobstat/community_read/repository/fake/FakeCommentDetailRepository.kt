package com.example.jobstat.community_read.repository.fake

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.repository.CommentDetailRepository
import com.example.jobstat.community_read.repository.impl.RedisCommentDetailRepository
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_serializer.DataSerializer
import org.springframework.data.redis.connection.StringRedisConnection
import java.util.concurrent.ConcurrentHashMap

internal class FakeCommentDetailRepository(
    private val dataSerializer: DataSerializer,
) : CommentDetailRepository {
    val store = ConcurrentHashMap<String, String>()

    private fun detailKey(commentId: Long) = RedisCommentDetailRepository.detailKey(commentId)

    override fun findCommentDetail(commentId: Long): CommentReadModel? =
        store[detailKey(commentId)]?.let {
            try {
                dataSerializer.deserialize(it, CommentReadModel::class)
            } catch (e: Exception) {
                null
            }
        }

    override fun findCommentDetails(commentIds: List<Long>): Map<Long, CommentReadModel> {
        if (commentIds.isEmpty()) return emptyMap()
        return commentIds.mapNotNull { id -> findCommentDetail(id)?.let { id to it } }.toMap()
    }

    override fun saveCommentDetail(
        comment: CommentReadModel,
        eventTs: Long,
    ) {
        val json =
            try {
                dataSerializer.serialize(comment) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "댓글 ${comment.id}에 대한 직렬화 결과가 null입니다")
            } catch (e: Exception) {
                throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "댓글 ${comment.id} 직렬화 실패")
            }
        store[detailKey(comment.id)] = json
    }

    override fun saveCommentDetails(
        comments: List<CommentReadModel>,
        eventTs: Long,
    ) {
        if (comments.isEmpty()) return
        val jsonMap =
            comments.associate { comment ->
                val json =
                    try {
                        dataSerializer.serialize(comment) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "댓글 ${comment.id}에 대한 직렬화 결과가 null입니다")
                    } catch (e: Exception) {
                        throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "배치에서 댓글 ${comment.id} 직렬화 실패")
                    }
                detailKey(comment.id) to json
            }
        store.putAll(jsonMap)
    }

    override fun saveCommentDetailInPipeline(
        conn: StringRedisConnection,
        comment: CommentReadModel,
    ) {
        saveCommentDetail(comment, 0L)
    }

    fun getJson(commentId: Long): String? = store[detailKey(commentId)]

    fun clear() {
        store.clear()
    }
}
