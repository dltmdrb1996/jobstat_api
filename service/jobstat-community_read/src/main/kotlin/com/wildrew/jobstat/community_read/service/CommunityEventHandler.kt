package com.wildrew.jobstat.community_read.service

import com.wildrew.jobstat.core.core_event.model.payload.board.BoardCreatedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardUpdatedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardViewedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload

interface CommunityEventHandler {
    fun handleBoardCreated(payload: BoardCreatedEventPayload)

    fun handleBoardUpdated(payload: BoardUpdatedEventPayload)

    fun handleBoardDeleted(payload: BoardDeletedEventPayload)

    fun handleBoardLiked(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload)

    fun handleBoardViewed(payload: BoardViewedEventPayload)

    fun handleBoardRankingUpdated(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload)

    fun handleCommentCreated(payload: com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload)

    fun handleCommentUpdated(payload: CommentUpdatedEventPayload)

    fun handleCommentDeleted(payload: CommentDeletedEventPayload)
}
