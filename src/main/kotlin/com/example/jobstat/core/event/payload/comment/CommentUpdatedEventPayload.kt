package com.example.jobstat.core.event.payload.comment

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CommentUpdatedEventPayload(
    @JsonProperty("commentId")
    val commentId: Long,
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload
