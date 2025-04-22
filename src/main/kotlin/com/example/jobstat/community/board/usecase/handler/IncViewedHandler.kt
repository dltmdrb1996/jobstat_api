package com.example.jobstat.community.board.usecase.handler // Command 서비스 패키지 구조에 맞게 조정

import com.example.jobstat.community.counting.CounterService // CounterService 임포트 (경로 확인 필요)
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.IncViewEventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.springframework.stereotype.Component

@Component
internal class IncViewedHandler(
    private val counterService: CounterService, // CounterService 주입
) : EventHandlingUseCase<EventType, IncViewEventPayload, Unit>() {
    override val eventType: EventType = EventType.BOARD_INC_VIEW

    /**
     * BoardViewed 이벤트를 수신하여 조회수를 증가시키는 로직 실행
     */
    override fun execute(payload: IncViewEventPayload) {
        log.debug("BoardViewed 이벤트 처리 시작: boardId={}", payload.boardId)

        try {
            val newTotalViewCount = counterService.incrementViewCount(payload.boardId)

            log.debug(
                "Redis 조회수 증가 및 Pending 처리 완료: boardId={}, newTotalRedisAndViewCount={}",
                payload.boardId,
                newTotalViewCount,
            )
        } catch (e: Exception) {
            log.error(
                "BoardViewed 이벤트 처리 중 CounterService 오류 발생: boardId={}, error={}",
                payload.boardId,
                e.message,
                e,
            )
            throw e // 예외를 다시 던져 RetryableTopic이 재시도하도록 함
        }
    }
}
