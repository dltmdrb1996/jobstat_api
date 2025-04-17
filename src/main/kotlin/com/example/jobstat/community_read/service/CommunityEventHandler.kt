package com.example.jobstat.community_read.service

import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*

interface CommunityEventHandler {
    fun handleBoardCreated(payload: BoardCreatedEventPayload)

    fun handleBoardUpdated(payload: BoardUpdatedEventPayload)

    fun handleBoardDeleted(payload: BoardDeletedEventPayload)

    fun handleBoardLiked(payload: BoardLikedEventPayload)

    fun handleBoardViewed(payload: BoardViewedEventPayload)

    fun handleBoardRankingUpdated(payload: BoardRankingUpdatedEventPayload)

    fun handleCommentCreated(payload: CommentCreatedEventPayload)

    fun handleCommentUpdated(payload: CommentUpdatedEventPayload)

    fun handleCommentDeleted(payload: CommentDeletedEventPayload)
}
