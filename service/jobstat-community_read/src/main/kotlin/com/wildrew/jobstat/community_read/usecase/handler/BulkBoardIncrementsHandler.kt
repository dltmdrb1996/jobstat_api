package com.wildrew.jobstat.community_read.usecase.handler

import com.wildrew.jobstat.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.board.BulkBoardIncrementsPayload
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BulkBoardIncrementsHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, BulkBoardIncrementsPayload, Unit>() {
    override val eventType: EventType = EventType.BULK_BOARD_INCREMENTS

    @Transactional
    override fun execute(payload: BulkBoardIncrementsPayload) {
        communityEventHandler.handleBulkBoardIncrements(payload)
    }
}
