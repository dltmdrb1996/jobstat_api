package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.payload.board.BoardLikedEventPayload
import org.springframework.stereotype.Component

@Component
class BoardLikedHandler(
    private val communityReadService: CommunityReadService
) : EventHandlingUseCase<EventType, BoardLikedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_LIKED

    override fun execute(payload: BoardLikedEventPayload) {
        communityReadService.handleBoardLiked(payload)
    }
}