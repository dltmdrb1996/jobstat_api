package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.core_event.model.EventType
import com.example.jobstat.core.core_event.model.payload.board.BoardDeletedEventPayload
import com.example.jobstat.core.core_event.consumer.EventHandlingUseCase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class BoardDeletedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BoardDeletedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_DELETED

    override fun execute(payload: BoardDeletedEventPayload) {
        communityEventHandler.handleBoardDeleted(payload)
    }
}
