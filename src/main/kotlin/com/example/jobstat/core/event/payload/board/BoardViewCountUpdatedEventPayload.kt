package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 게시글 조회수 업데이트 이벤트 페이로드
 */
data class BoardViewCountUpdatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    
    @JsonProperty("viewCount")
    val viewCount: Int,
    
    @JsonProperty("incrementAmount")
    val incrementAmount: Int,
    
    @JsonProperty("timestamp")
    val timestamp: Long
) : EventPayload