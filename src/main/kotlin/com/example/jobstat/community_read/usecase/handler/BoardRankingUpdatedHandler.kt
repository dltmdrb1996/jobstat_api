package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardRankingUpdatedEventPayload
import org.springframework.stereotype.Component

@Component
class BoardRankingUpdatedHandler(
    private val communityEventHandler: CommunityEventHandler
) : EventHandlingUseCase<EventType, BoardRankingUpdatedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_RANKING_UPDATED

    override fun execute(payload: BoardRankingUpdatedEventPayload) {
        communityEventHandler.handleBoardRankingUpdated(payload)
    }
}
