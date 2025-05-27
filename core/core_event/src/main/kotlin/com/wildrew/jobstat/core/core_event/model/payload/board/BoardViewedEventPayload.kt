package com.wildrew.jobstat.core.core_event.model.payload.board

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload
import java.time.LocalDateTime

data class BoardViewedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("viewCount")
    val viewCount: Int,
) : EventPayload
