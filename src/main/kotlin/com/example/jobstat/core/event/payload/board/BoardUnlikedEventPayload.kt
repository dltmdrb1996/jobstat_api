package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 게시글 좋아요 취소 이벤트 페이로드
 */
data class BoardUnlikedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    
    @JsonProperty("userId")
    val userId: Long,

    @JsonProperty("likeCount")
    val likeCount: Int = 1
) : EventPayload

