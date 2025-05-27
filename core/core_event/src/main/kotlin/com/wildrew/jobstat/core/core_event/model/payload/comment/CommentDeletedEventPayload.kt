package com.wildrew.jobstat.core.core_event.model.payload.comment

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload

data class CommentDeletedEventPayload(
    @JsonProperty("commentId")
    val commentId: Long,
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload
