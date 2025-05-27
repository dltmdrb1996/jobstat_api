package com.wildrew.jobstat.community_read.repository

import com.wildrew.jobstat.community_read.model.CommentReadModel
import org.springframework.data.redis.connection.StringRedisConnection

interface CommentDetailRepository {
    fun findCommentDetail(commentId: Long): CommentReadModel?

    fun findCommentDetails(commentIds: List<Long>): Map<Long, CommentReadModel>

    fun saveCommentDetail(
        comment: CommentReadModel,
        eventTs: Long,
    )

    fun saveCommentDetails(
        comments: List<CommentReadModel>,
        eventTs: Long,
    )

    fun saveCommentDetailInPipeline(
        conn: StringRedisConnection,
        comment: CommentReadModel,
    )
}
