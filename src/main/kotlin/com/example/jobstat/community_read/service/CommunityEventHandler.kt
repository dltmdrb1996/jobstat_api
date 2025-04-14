package com.example.jobstat.community_read.service

import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*

/**
 * 커뮤니티 이벤트 핸들러 인터페이스
 * 게시글과 댓글 이벤트 처리 기능을 정의합니다.
 */
interface CommunityEventHandler {
    // 게시글 기본 이벤트
    fun handleBoardCreated(payload: BoardCreatedEventPayload)

    fun handleBoardUpdated(payload: BoardUpdatedEventPayload)

    fun handleBoardDeleted(payload: BoardDeletedEventPayload)

    // 게시글 상호작용 이벤트
    fun handleBoardLiked(payload: BoardLikedEventPayload)

    fun handleBoardViewed(payload: BoardViewedEventPayload)

    // 게시글 랭킹 이벤트
    fun handleBoardRankingUpdated(payload: BoardRankingUpdatedEventPayload)

    // 댓글 이벤트 핸들러 메서드들
    // 댓글 기본 이벤트
    fun handleCommentCreated(payload: CommentCreatedEventPayload)

    fun handleCommentUpdated(payload: CommentUpdatedEventPayload)

    fun handleCommentDeleted(payload: CommentDeletedEventPayload)
}
