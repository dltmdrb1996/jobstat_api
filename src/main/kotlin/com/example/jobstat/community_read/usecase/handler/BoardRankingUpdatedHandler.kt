package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.core_event.model.EventType
import com.example.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload
import com.example.jobstat.core.core_usecase.base.EventHandlingUseCase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class BoardRankingUpdatedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BoardRankingUpdatedEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_RANKING_UPDATED

    override fun execute(payload: BoardRankingUpdatedEventPayload) {
        communityEventHandler.handleBoardRankingUpdated(payload)
    }
}
