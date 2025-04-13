package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardViewedEventPayload
import org.springframework.stereotype.Component

@Component
class BoardViewedHandler(
    private val communityEventHandler: CommunityEventHandler
) : EventHandlingUseCase<EventType, BoardViewedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_VIEWED

    override fun execute(payload: BoardViewedEventPayload) {
        communityEventHandler.handleBoardViewed(payload)
    }
}