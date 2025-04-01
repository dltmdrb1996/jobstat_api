package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.board.BoardViewCountUpdatedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 게시판 조회수 업데이트 이벤트 처리 유스케이스
 * 조회수 이벤트가 발생하면 읽기 모델의 조회수 캐시를 업데이트
 */
@Component
@EventHandler(eventType = EventType.BOARD_VIEW_COUNT_UPDATED)
class HandleBoardViewCountUpdatedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleBoardViewCountUpdatedUseCase.Request, HandleBoardViewCountUpdatedUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 이벤트 처리
     */
    fun handle(event: Event<out EventPayload>) {
        val payload = event.payload as BoardViewCountUpdatedEventPayload
        
        val request = Request(
            boardId = payload.boardId,
            viewCount = payload.viewCount,
            createdAt = payload.createdAt?.toEpochMilli() ?: System.currentTimeMillis()
        )
        
        execute(request)
    }
    
    /**
     * 읽기 모델 업데이트 실행
     */
    override fun execute(request: Request): Response {
        log.info("[HandleBoardViewCountUpdatedUseCase] 게시글 조회수 업데이트: boardId={}, viewCount={}", 
            request.boardId, request.viewCount)
        
        // 읽기 모델 업데이트
        communityReadService.updateBoardViewCount(request.boardId, request.viewCount, request.createdAt)
        
        return Response(success = true)
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val boardId: Long,
        
        @field:Min(value = 0, message = "조회수는 0 이상이어야 합니다.")
        val viewCount: Int,
        
        val createdAt: Long
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean
    )
} 