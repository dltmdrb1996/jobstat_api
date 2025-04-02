package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardCreatedEventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 게시판 생성 이벤트 핸들러
 */
@Component
class BoardCreatedHandler(
    private val communityReadService: CommunityReadService
) : EventHandlingUseCase<EventType, BoardCreatedEventPayload, Unit>() {


    override val eventType: EventType = EventType.BOARD_CREATED

    @Transactional
    override fun execute(payload: BoardCreatedEventPayload) {
        communityReadService.handleBoardCreated(payload)
    }


}