package com.wildrew.app.community_read.repository

import com.wildrew.jobstat.core.core_event.model.payload.board.BoardCreatedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardUpdatedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardViewedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload

interface CommunityEventUpdateRepository {
    fun applyBoardCreation(payload: BoardCreatedEventPayload): Boolean

    fun applyBoardUpdate(
        payload: BoardUpdatedEventPayload,
        updatedBoardJson: String,
    ): Boolean

    fun applyBoardDeletion(payload: BoardDeletedEventPayload): Boolean

    fun applyBoardLikeUpdate(
        payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload,
        updatedBoardJson: String,
    ): Boolean

    fun applyBoardViewUpdate(
        payload: BoardViewedEventPayload,
        updatedBoardJson: String,
    ): Boolean

    fun applyBoardRankingUpdate(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload): Boolean

    fun applyCommentCreation(
        payload: com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload,
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
