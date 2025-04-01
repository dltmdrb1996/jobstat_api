package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload

// 게시글 좋아요 수 감소 이벤트 페이로드
data class BoardLikeCountDecreasedEventPayload(
    val boardId: Long,
    val likeCount: Int,
    val decrementAmount: Int,
    val timestamp: Long = System.currentTimeMillis()
) : EventPayload