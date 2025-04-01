package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.comment.CommentUpdatedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 댓글 수정 이벤트 처리 유스케이스
 */
@Component
@EventHandler(eventType = EventType.COMMENT_UPDATED)
class HandleCommentUpdatedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleCommentUpdatedUseCase.Request, HandleCommentUpdatedUseCase.Response>(validator) {
    
    /**
     * 이벤트 처리 요청 수신 및 유효성 검사
     */
    fun handle(event: Event<out EventPayload>) {
        (event.payload as CommentUpdatedEventPayload).let { payload ->
            execute(
                Request(
                commentId = payload.commentId,
                boardId = payload.boardId,
                content = payload.content,
                author = payload.author,
                isDeleted = payload.deleted
            )
            )
        }
    }
    
    /**
     * 유스케이스 실행
     */
    override fun execute(request: Request): Response = with(request) {
        // 댓글 조회
        val existingComment = communityReadService.getCommentById(commentId)
            ?: return Response(success = false, comment = null)
        
        // 댓글 업데이트
        val updatedComment = updateComment(existingComment, request)
        communityReadService.updateComment(updatedComment)
        
        return Response(success = true, comment = updatedComment)
    }
    
    private fun updateComment(existingComment: CommentReadModel, request: Request): CommentReadModel = with(request) {
        existingComment.copy(
            content = content ?: existingComment.content,
            author = author ?: existingComment.author,
            updatedAt = LocalDateTime.now(),
            isDeleted = isDeleted ?: existingComment.isDeleted
        )
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val commentId: Long,
        val boardId: Long,
        val content: String? = null,
        val author: String? = null,
        val isDeleted: Boolean? = null
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean,
        val comment: CommentReadModel? = null
    )
} 