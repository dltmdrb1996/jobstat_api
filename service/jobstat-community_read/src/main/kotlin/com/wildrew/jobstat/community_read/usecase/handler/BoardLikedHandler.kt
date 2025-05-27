package com.wildrew.jobstat.community_read.usecase.handler

import com.wildrew.jobstat.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class BoardLikedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_LIKED

    override fun execute(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload) {
        communityEventHandler.handleBoardLiked(payload)
    }
}
