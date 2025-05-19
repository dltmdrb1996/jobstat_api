package com.example.jobstat.core.core_event.model.payload.comment

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.core.core_event.model.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CommentCreatedEventPayload(
    @JsonProperty("commentId")
    val commentId: Long,
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("userId")
    val userId: Long? = null,
    @JsonProperty("author")
    val author: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload {
    fun toReadModel() =
        CommentReadModel(
            id = commentId,
            boardId = boardId,
            userId = userId,
            author = author,
            content = content,
            createdAt = createdAt,
            updatedAt = createdAt,
            eventTs = eventTs,
        )
}
