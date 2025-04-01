package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.CommentReadModel
import java.time.Duration

interface CommentDetailRepository {
    fun create(commentModel: CommentReadModel, ttl: Duration = Duration.ofDays(1))
    fun update(commentModel: CommentReadModel)
    fun delete(commentId: Long)
    fun read(commentId: Long): CommentReadModel?
    fun readAll(commentIds: List<Long>): Map<Long, CommentReadModel>
}