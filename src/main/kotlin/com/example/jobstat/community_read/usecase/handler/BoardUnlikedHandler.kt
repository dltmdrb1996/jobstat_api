package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.board.BoardUnlikedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 게시판 좋아요 취소 이벤트 처리 유스케이스
 */
@Component
@EventHandler(eventType = EventType.BOARD_UNLIKED)
class HandleBoardUnlikedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleBoardUnlikedUseCase.Request, HandleBoardUnlikedUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 이벤트 처리
     */
    fun handle(event: Event<out EventPayload>) {
        val payload = event.payload as BoardUnlikedEventPayload
        
        // 요청 객체 생성 및 유효성 검사
        val request = Request(
            boardId = payload.boardId,
            userId = payload.userId,
            likeCount = payload.likeCount,
            createdAt = payload.createdAt?.toEpochMilli() ?: System.currentTimeMillis()
        )
        
        // 유스케이스 실행
        execute(request)
    }
    
    /**
     * 유스케이스 실행
     */
    override fun execute(request: Request): Response {
        log.info("[HandleBoardUnlikedUseCase] 게시글 좋아요 취소 이벤트 처리: boardId={}, userId={}, likeCount={}", 
            request.boardId, request.userId, request.likeCount)
        
        // 좋아요 취소 처리
        if (request.likeCount > 0) {
            // 이벤트에서 받은 좋아요 수 직접 사용
            communityReadService.updateBoardLikeCount(request.boardId, request.likeCount, request.createdAt)
            log.info("[HandleBoardUnlikedUseCase] 게시글 좋아요 수 직접 업데이트: boardId={}, likeCount={}", 
                request.boardId, request.likeCount)
        } else {
            // 좋아요 수가 전달되지 않은 경우 직접 취소 처리
            val board = communityReadService.getBoardById(request.boardId)
            if (board != null) {
                communityReadService.removeBoardLike(request.boardId, request.userId)
                log.info("[HandleBoardUnlikedUseCase] 게시글 좋아요 취소 완료: boardId={}", request.boardId)
            } else {
                log.warn("[HandleBoardUnlikedUseCase] 좋아요 취소할 게시글을 찾을 수 없음: boardId={}", request.boardId)
            }
        }
        
        return Response(success = true)
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val boardId: Long,
        val userId: String,
        val likeCount: Int = 0,
        val createdAt: Long
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean
    )
} 