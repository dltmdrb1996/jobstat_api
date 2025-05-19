package com.example.jobstat.community_read.event

import com.example.jobstat.core.core_event.model.EventType
import com.example.jobstat.core.core_event.outbox.OutboxEventPublisher
import com.example.jobstat.core.core_event.model.payload.board.IncViewEventPayload
import com.example.jobstat.core.core_event.publisher.AbstractEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CommunityReadEventPublisher(
    outboxEventPublisher: OutboxEventPublisher,
) : AbstractEventPublisher(outboxEventPublisher) {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun getSupportedEventTypes(): Set<EventType> = SUPPORTED_EVENT_TYPES

    companion object {
        private val SUPPORTED_EVENT_TYPES =
            setOf(
                EventType.BOARD_INC_VIEW,
            )
    }

    fun publishIncViewed(
        boardId: Long,
        delta: Int,
        eventTs: Long = System.currentTimeMillis(),
    ) {
        log.debug("게시글 조회수 증가 이벤트 발행 준비: boardId=$boardId")
        log.debug("@@@ 게시글 조회수 증가 이벤트 발행 준비: boardId=${EventType.BOARD_INC_VIEW}")
        val payload =
            IncViewEventPayload(
                boardId = boardId,
                eventTs = eventTs,
                delta = delta,
            )
        publish(EventType.BOARD_INC_VIEW, payload)
    }
}
