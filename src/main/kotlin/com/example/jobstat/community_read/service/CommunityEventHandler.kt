package com.example.jobstat.community_read.service

import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*

/**
 * 커뮤니티 이벤트 핸들러 인터페이스
 * 게시글과 댓글 이벤트 처리 기능을 정의합니다.
 */
interface CommunityEventHandler {
    // 게시글 이벤트 핸들러 메서드들
    fun handleBoardCreated(payload: BoardCreatedEventPayload)
    fun handleBoardUpdated(payload: BoardUpdatedEventPayload)
    fun handleBoardDeleted(payload: BoardDeletedEventPayload)
    fun handleBoardLiked(payload: BoardLikedEventPayload)
    fun handleBoardViewed(payload: BoardViewedEventPayload)
    fun handleBoardRankingUpdated(payload: BoardRankingUpdatedEventPayload)

    // 댓글 이벤트 핸들러 메서드들
    fun handleCommentCreated(payload: CommentCreatedEventPayload)
    fun handleCommentUpdated(payload: CommentUpdatedEventPayload)
    fun handleCommentDeleted(payload: CommentDeletedEventPayload)
}