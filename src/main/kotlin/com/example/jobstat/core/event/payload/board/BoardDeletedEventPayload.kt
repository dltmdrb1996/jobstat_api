package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 게시글 삭제 이벤트 페이로드
 */
data class BoardDeletedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,

    @JsonProperty("userId")
    val userId: String? = null
) : EventPayload