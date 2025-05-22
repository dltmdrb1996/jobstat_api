package com.wildrew.jobstat.core.core_event.model.payload.comment

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload
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
) : EventPayload