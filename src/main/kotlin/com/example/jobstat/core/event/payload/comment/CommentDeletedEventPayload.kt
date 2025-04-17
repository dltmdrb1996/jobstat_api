package com.example.jobstat.core.event.payload.comment

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

data class CommentDeletedEventPayload(
    @JsonProperty("commentId")
    val commentId: Long,
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload
