package com.wildrew.jobstat.core.core_event.model.payload.board

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload
import java.time.LocalDateTime

data class BoardCreatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("userId")
    val userId: Long? = null,
    @JsonProperty("categoryId")
    val categoryId: Long,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("author")
    val author: String,
) : EventPayload