package com.example.jobstat.community_read.event // 패키지명은 실제 구조에 맞게 조정하세요.

import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.outbox.OutboxEventPublisher
import com.example.jobstat.core.event.payload.board.IncViewEventPayload
import com.example.jobstat.core.event.publisher.AbstractEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 커뮤니티 Read 서비스 관련 이벤트 발행 담당
 * (예: 게시글 조회 이벤트)
 */
@Component
class CommunityReadEventPublisher(
    outboxEventPublisher: OutboxEventPublisher // OutboxPublisher 주입
) : AbstractEventPublisher(outboxEventPublisher) {

    private val log = LoggerFactory.getLogger(this::class.java)

    // 이 퍼블리셔는 BOARD_VIEWED 이벤트만 발행한다고 가정
    override fun getSupportedEventTypes(): Set<EventType> = SUPPORTED_EVENT_TYPES

    companion object {
        private val SUPPORTED_EVENT_TYPES = setOf(
            EventType.BOARD_INC_VIEW
        )
    }

    fun publishIncViewed(
        boardId: Long,
        delta: Int,
        eventTs: Long = System.currentTimeMillis(),
    ) {
        log.info("게시글 조회수 증가 이벤트 발행 준비: boardId=${boardId}")
        log.info("@@@ 게시글 조회수 증가 이벤트 발행 준비: boardId=${EventType.BOARD_INC_VIEW}")
        val payload = IncViewEventPayload(
            boardId = boardId,
            eventTs = eventTs,
            delta = delta, // 이벤트 페이로드에 증가할 조회수 포함
        )
        publish(EventType.BOARD_INC_VIEW, payload)
    }

}