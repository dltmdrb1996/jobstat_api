package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.comment.CommentCreatedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 댓글 생성 이벤트 처리 유스케이스
 */
@Component
@EventHandler(eventType = EventType.COMMENT_CREATED)
class HandleCommentCreatedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleCommentCreatedUseCase.Request, HandleCommentCreatedUseCase.Response>(validator) {
    
    /**
     * 이벤트 처리 요청 수신 및 유효성 검사
     */
    fun handle(event: Event<out EventPayload>) {
        (event.payload as CommentCreatedEventPayload).let { payload ->
            execute(
                Request(
                commentId = payload.commentId,
                boardId = payload.boardId,
                content = payload.content ?: "",
                author = payload.author ?: "Unknown",
                userId = payload.writerId,
                createdAt = payload.createdAt ?: LocalDateTime.now(),
                isDeleted = payload.deleted ?: false
            )
            )
        }
    }
    
    /**
     * 유스케이스 실행
     */
    override fun execute(request: Request): Response = with(request) {
        createCommentReadModel(request)
            .let { communityReadService.createComment(it) }
            .let { Response(success = true, commentCount = communityReadService.incrementBoardCommentCount(boardId)) }
    }
    
    private fun createCommentReadModel(request: Request): CommentReadModel = with(request) {
        CommentReadModel(
            id = commentId,
            boardId = boardId,
            content = content,
            author = author,
            userId = userId,
            likeCount = 0,
            createdAt = createdAt,
            updatedAt = LocalDateTime.now(),
            isDeleted = isDeleted
        )
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val commentId: Long,
        val boardId: Long,
        val content: String,
        val author: String,
        val userId: Long? = null,
        val createdAt: LocalDateTime,
        val isDeleted: Boolean = false
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean,
        val comment: CommentReadModel? = null,
        val commentCount: Int = 0
    )
} 