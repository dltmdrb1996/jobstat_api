package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardViewedEventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class BoardViewedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BoardViewedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_VIEWED

    override fun execute(payload: BoardViewedEventPayload) {
        communityEventHandler.handleBoardViewed(payload)
    }
}
