package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.payload.board.BoardUnlikedEventPayload
import org.springframework.stereotype.Component

@Component
class BoardUnlikedHandler(
    private val communityReadService: CommunityReadService
) : EventHandlingUseCase<EventType, BoardUnlikedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_UNLIKED

    override fun execute(payload: BoardUnlikedEventPayload) {
        communityReadService.handleBoardLiked(payload)
    }
}