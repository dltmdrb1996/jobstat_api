package com.example.jobstat.core.event.payload

import com.example.jobstat.core.event.EventPayload
import java.time.LocalDateTime

data class CommentDeletedEventPayload(
    val commentId: Long,
    val content: String,
    val path: String,
    val articleId: Long,
    val writerId: Long,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
    val articleCommentCount: Long
) : EventPayload