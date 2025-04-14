package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 게시글 조회수 업데이트 이벤트 페이로드
 */
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
