package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardDeletedEventPayload
import org.springframework.stereotype.Component


@Component
class BoardDeletedHandler(
    private val communityReadService: CommunityReadService
) : EventHandlingUseCase<EventType, BoardDeletedEventPayload, Unit>() {

    override val eventType: EventType = EventType.BOARD_DELETED

    override fun execute(payload: BoardDeletedEventPayload) {
        communityReadService.handleBoardDeleted(payload)
    }
}