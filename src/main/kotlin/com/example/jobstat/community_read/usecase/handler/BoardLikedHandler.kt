package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.board.BoardLikedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 게시판 좋아요 이벤트 처리 유스케이스
 */
@Component
@EventHandler(eventType = EventType.BOARD_LIKED)
class HandleBoardLikedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleBoardLikedUseCase.Request, HandleBoardLikedUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 이벤트 처리
     */
    fun handle(event: Event<out EventPayload>) {
        val payload = event.payload as BoardLikedEventPayload

        val request = Request(
            boardId = payload.boardId,
            likeCount = payload.likeCount,
            userId = payload.userId,
            createdAt = payload.createdAt?.toEpochMilli() ?: System.currentTimeMillis()
        )

        execute(request)
    }

    /**
     * 읽기 모델 업데이트 실행
     */
    override fun execute(request: Request): Response {
        log.info("[HandleBoardLikedUseCase] 게시글 좋아요 처리: boardId={}, likeCount={}, userId={}",
            request.boardId, request.likeCount, request.userId)

        try {
            // 읽기 모델 업데이트
            communityReadService.updateBoardLikeCount(request.boardId, request.likeCount, request.createdAt)

            // 사용자별 좋아요 상태 업데이트
            communityReadService.updateUserLikeStatus(request.boardId, request.userId, true)

            return Response(success = true)
        } catch (e: Exception) {
            log.error("[HandleBoardLikedUseCase] 게시글 좋아요 처리 실패: boardId={}, error={}",
                request.boardId, e.message, e)
            return Response(success = false)
        }
    }

    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val boardId: Long,

        @field:Min(value = 0, message = "좋아요 수는 0 이상이어야 합니다.")
        val likeCount: Int,

        val userId: String,

        val createdAt: Long
    )

    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean
    )
}