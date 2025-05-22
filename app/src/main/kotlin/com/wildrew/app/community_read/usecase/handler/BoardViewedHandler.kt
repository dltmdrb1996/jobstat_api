package com.wildrew.app.community_read.usecase.handler

import com.wildrew.app.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardViewedEventPayload
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
