package com.example.jobstat.core.event.payload.comment

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 게시글 좋아요 수 업데이트 이벤트 페이로드
 */
data class BoardLikeCountUpdatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    
    @JsonProperty("likeCount")
    val likeCount: Int,
    
    @JsonProperty("incrementAmount")
    val incrementAmount: Int,
    
    @JsonProperty("timestamp")
    val timestamp: Long
) : EventPayload