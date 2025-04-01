package com.example.jobstat.community.comment.usecase

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 댓글 생성 알림 유스케이스
 */
@Component
class NotifyCommentCreated {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    
    data class Request(
        val commentId: Long,
        val boardId: Long,
        val content: String,
        val author: String
    )
    
    data class Response(
        val success: Boolean
    )
    
    operator fun invoke(request: Request): Response = with(request) {
        // 기본 로깅
        log.info("[NotifyCommentCreated] commentId={}, boardId={}, author={}", 
            commentId, boardId, author)
        
        // 알림 발송 로직 (여기서는 로그만 남기는 것으로 대체)
        log.info("[NotifyCommentCreated] 댓글 알림 발송: '{}'님이 새 댓글을 작성했습니다: {}", 
            author, content)
        
        // 성공 응답 반환
        Response(true)
    }
} 