package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 게시글 생성 이벤트 페이로드
 */
data class BoardCreatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("userId")
    val userId: String? = null,
    @JsonProperty("categoryId")
    val categoryId: Long,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("author")
    val author: String,
) : EventPayload