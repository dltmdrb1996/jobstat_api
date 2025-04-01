package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 게시글 업데이트 이벤트 페이로드
 */
data class BoardUpdatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("author")
    val author: String,
) : EventPayload