package com.example.jobstat.community_read.service

import com.example.jobstat.core.core_event.model.payload.board.*
import com.example.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload
import com.example.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.example.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload

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
