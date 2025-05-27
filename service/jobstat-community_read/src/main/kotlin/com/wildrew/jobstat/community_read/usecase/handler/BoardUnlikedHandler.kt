package com.wildrew.jobstat.community_read.usecase.handler

import com.wildrew.jobstat.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardUnlikedEventPayload
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class BoardUnlikedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BoardUnlikedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_UNLIKED

    override fun execute(payload: BoardUnlikedEventPayload) {
        communityEventHandler.handleBoardLiked(
            com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload(
                boardId = payload.boardId,
                createdAt = payload.createdAt,
                eventTs = payload.eventTs,
                userId = payload.userId,
                likeCount = payload.likeCount,
            ),
        )
    }
}
