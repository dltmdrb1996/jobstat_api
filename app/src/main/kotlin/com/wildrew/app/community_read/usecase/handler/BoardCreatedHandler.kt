package com.wildrew.app.community_read.usecase.handler

import com.wildrew.app.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.board.BoardCreatedEventPayload
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BoardCreatedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BoardCreatedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_CREATED

    @Transactional
    override fun execute(payload: BoardCreatedEventPayload) {
        communityEventHandler.handleBoardCreated(payload)
    }
}
