package com.example.jobstat.core.event.payload.comment

import com.example.jobstat.core.event.EventPayload
import java.time.LocalDateTime

data class CommentUpdatedEventPayload(
    val commentId: Long,
    val boardId : Long,
    val content: String,
    val path: String,
    val author : String,
    val articleId: Long,
    val writerId: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
    val articleCommentCount: Long
) : EventPayload