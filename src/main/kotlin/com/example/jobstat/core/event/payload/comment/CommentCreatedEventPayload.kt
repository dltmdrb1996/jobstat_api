package com.example.jobstat.core.event.payload.comment

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 댓글 생성 이벤트 페이로드
 */
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
    val eventTs: Long
) : EventPayload {
    fun toReadModel() = CommentReadModel(
        id = commentId,
        boardId = boardId,
        userId = userId,
        author = author,
        content = content,
        createdAt = createdAt,
        updatedAt = createdAt,
        eventTs = eventTs
    )
}


