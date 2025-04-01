package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.comment.CommentDeletedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import org.springframework.stereotype.Component

/**
 * 댓글 삭제 이벤트 처리 유스케이스
 */
@Component
@EventHandler(eventType = EventType.COMMENT_DELETED)
class HandleCommentDeletedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleCommentDeletedUseCase.Request, HandleCommentDeletedUseCase.Response>(validator) {
    
    /**
     * 이벤트 처리 요청 수신 및 유효성 검사
     */
    fun handle(event: Event<out EventPayload>) {
        (event.payload as CommentDeletedEventPayload).let { payload ->
            execute(
                Request(
                commentId = payload.commentId,
                boardId = payload.boardId,
                hardDelete = payload.hardDelete ?: false
            )
            )
        }
    }
    
    /**
     * 유스케이스 실행
     */
    override fun execute(request: Request): Response = with(request) {
        // 댓글 삭제 처리
        communityReadService.deleteComment(commentId)
        
        // 게시글 댓글 수 업데이트
        val commentCount = communityReadService.decrementBoardCommentCount(boardId)
        Response(success = true, commentCount = commentCount)
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val commentId: Long,
        val boardId: Long,
        val hardDelete: Boolean = false
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean,
        val commentCount: Int = 0
    )
} 