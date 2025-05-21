package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.core_event.model.EventType
import com.example.jobstat.core.core_event.model.payload.board.BoardUpdatedEventPayload
import com.example.jobstat.core.core_event.consumer.EventHandlingUseCase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class BoardUpdatedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BoardUpdatedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_UPDATED

    override fun execute(payload: BoardUpdatedEventPayload) {
        communityEventHandler.handleBoardUpdated(payload)
    }
}
