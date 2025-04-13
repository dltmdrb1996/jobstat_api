package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.core.event.EventType
import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.event.payload.board.BoardLikedEventPayload
import com.example.jobstat.core.event.payload.board.BoardUnlikedEventPayload
import org.springframework.stereotype.Component

@Component
class BoardUnlikedHandler(
    private val communityEventHandler: CommunityEventHandler
) : EventHandlingUseCase<EventType, BoardUnlikedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_UNLIKED

    override fun execute(payload: BoardUnlikedEventPayload) {
        communityEventHandler.handleBoardLiked(
            BoardLikedEventPayload(
                boardId = payload.boardId,
                createdAt = payload.createdAt,
                eventTs = payload.eventTs,
                userId = payload.userId,
                likeCount = payload.likeCount
            )
        )
    }
}