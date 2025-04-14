package com.example.jobstat.core.event.payload.comment

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 댓글 수정 이벤트 페이로드
 */
data class CommentUpdatedEventPayload(
    @JsonProperty("commentId")
    val commentId: Long,
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload
