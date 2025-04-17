package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

data class IncViewEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("delta")
    val delta: Int,
) : EventPayload
