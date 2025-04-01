package com.example.jobstat.community_read.handler

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardDeletedEventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 게시판 삭제 이벤트 핸들러
 */
@Component
class BoardDeletedHandler(
    private val communityReadService: CommunityReadService
) : EventHandlingUseCase<EventType, BoardDeletedEventPayload, BoardDeletedHandler.Response>() {

    /**
     * 이 핸들러가 처리하는 이벤트 타입
     */
    override val eventType: EventType = EventType.BOARD_DELETED

    /**
     * 페이로드 유효성 검사
     */
    override fun validatePayload(payload: BoardDeletedEventPayload) {
        require(payload.boardId > 0) { "게시글 ID는 0보다 커야 합니다" }
    }

    /**
     * 비즈니스 로직 실행
     */
    @Transactional
    override fun execute(payload: BoardDeletedEventPayload): Response {
        communityReadService.deleteBoard(payload.boardId)
        return Response(success = true)
    }

    /**
     * 오류 응답 생성
     */
    override fun createErrorResponse(e: Exception): Response {
        return Response(success = false)
    }

    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean
    )
}