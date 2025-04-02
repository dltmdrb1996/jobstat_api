package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardUpdatedEventPayload
import org.springframework.stereotype.Component

//@Component
//class BoardUpdatedHandler(
//    private val communityReadService: CommunityReadService
//) : EventHandlingUseCase<EventType, BoardUpdatedEventPayload, Unit>() {
//
//    override val eventType: EventType = EventType.BOARD_UPDATED
//
//    override fun execute(payload: BoardUpdatedEventPayload) {
//        communityReadService.handleBoardUpdated(payload)
//    }
//}