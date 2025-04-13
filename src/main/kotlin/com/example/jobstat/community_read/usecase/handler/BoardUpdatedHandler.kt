package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardUpdatedEventPayload
import org.springframework.stereotype.Component

@Component
class BoardUpdatedHandler(
    private val communityEventHandler: CommunityEventHandler
) : EventHandlingUseCase<EventType, BoardUpdatedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_UPDATED

    override fun execute(payload: BoardUpdatedEventPayload) {
        communityEventHandler.handleBoardUpdated(payload)
    }
}