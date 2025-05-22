package com.wildrew.app.community_read.model

import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload
import java.time.LocalDateTime

data class CommentReadModel(
    val id: Long,
    val boardId: Long,
    val userId: Long? = null,
    val author: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null,
    val eventTs: Long,
) {
    companion object {
        fun fromPayload(payload: CommentCreatedEventPayload): CommentReadModel {
            return CommentReadModel(
                id = payload.commentId,
                boardId = payload.boardId,
                userId = payload.userId,
                author = payload.author,
                content = payload.content,
                createdAt = payload.createdAt,
                eventTs = payload.eventTs,
            )
        }
    }
}
