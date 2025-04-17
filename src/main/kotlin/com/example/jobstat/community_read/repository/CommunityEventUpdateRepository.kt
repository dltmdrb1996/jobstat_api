package com.example.jobstat.community_read.repository

import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*

interface CommunityEventUpdateRepository {
    fun applyBoardCreation(payload: BoardCreatedEventPayload): Boolean

    fun applyBoardUpdate(
        payload: BoardUpdatedEventPayload,
        updatedBoardJson: String,
    ): Boolean

    fun applyBoardDeletion(payload: BoardDeletedEventPayload): Boolean

    fun applyBoardLikeUpdate(
        payload: BoardLikedEventPayload,
        updatedBoardJson: String,
    ): Boolean

    fun applyBoardViewUpdate(
        payload: BoardViewedEventPayload,
        updatedBoardJson: String,
    ): Boolean

    fun applyBoardRankingUpdate(payload: BoardRankingUpdatedEventPayload): Boolean

    fun applyCommentCreation(
        payload: CommentCreatedEventPayload,
        commentJson: String,
        updatedBoardJson: String?,
    ): Boolean

    fun applyCommentUpdate(
        payload: CommentUpdatedEventPayload,
        updatedCommentJson: String,
    ): Boolean

    fun applyCommentDeletion(
        payload: CommentDeletedEventPayload,
        updatedBoardJson: String?,
    ): Boolean
}
