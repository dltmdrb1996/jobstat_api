package com.wildrew.app.community_read.model

import com.wildrew.jobstat.core.core_event.model.payload.board.BoardCreatedEventPayload
import java.time.LocalDateTime

data class BoardReadModel(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val author: String,
    val userId: Long?,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime,
    val eventTs: Long,
    var comments: List<CommentReadModel> = emptyList(),
) {
    companion object {
        fun fromPayload(payload: BoardCreatedEventPayload): BoardReadModel =
            BoardReadModel(
                id = payload.boardId,
                categoryId = payload.categoryId,
                title = payload.title,
                content = payload.content,
                author = payload.author,
                userId = payload.userId,
                viewCount = 0,
                likeCount = 0,
                commentCount = 0,
                createdAt = payload.createdAt,
                eventTs = payload.eventTs,
            )
    }
}
