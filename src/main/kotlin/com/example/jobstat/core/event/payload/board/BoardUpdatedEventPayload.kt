package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

data class BoardUpdatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("author")
    val author: String,
) : EventPayload
