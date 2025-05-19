package com.example.jobstat.core.core_event.model.payload.board

import com.example.jobstat.core.core_event.model.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class BoardUnlikedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("userId")
    val userId: Long,
    @JsonProperty("likeCount")
    val likeCount: Int,
) : EventPayload
