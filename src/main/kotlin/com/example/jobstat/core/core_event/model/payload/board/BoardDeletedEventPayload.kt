package com.example.jobstat.core.core_event.model.payload.board

import com.example.jobstat.core.core_event.model.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

data class BoardDeletedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("categoryId")
    val categoryId: Long,
    @JsonProperty("userId")
    val userId: Long? = null,
) : EventPayload
