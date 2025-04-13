package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityEventHandler
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.comment.CommentUpdatedEventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 댓글 수정 이벤트 핸들러
 */
@Component
class CommentUpdatedHandler(
    private val communityEventHandler: CommunityEventHandler
) : EventHandlingUseCase<EventType, CommentUpdatedEventPayload, Unit>() {

    override val eventType: EventType = EventType.COMMENT_UPDATED

    @Transactional
    override fun execute(payload: CommentUpdatedEventPayload) {
        communityEventHandler.handleCommentUpdated(payload)
    }
} 