package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class BoardLikedEventPayload(
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
