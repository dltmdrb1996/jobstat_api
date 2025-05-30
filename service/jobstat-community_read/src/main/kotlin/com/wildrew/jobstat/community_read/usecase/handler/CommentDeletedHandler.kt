package com.wildrew.jobstat.community_read.usecase.handler

import com.wildrew.jobstat.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CommentDeletedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, CommentDeletedEventPayload, Unit>() {
    override val eventType: EventType = EventType.COMMENT_DELETED

    @Transactional
    override fun execute(payload: CommentDeletedEventPayload) {
        communityEventHandler.handleCommentDeleted(payload)
    }
}
