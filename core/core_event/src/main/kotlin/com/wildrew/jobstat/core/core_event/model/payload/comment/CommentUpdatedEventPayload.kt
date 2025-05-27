package com.wildrew.jobstat.core.core_event.model.payload.comment

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload
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
