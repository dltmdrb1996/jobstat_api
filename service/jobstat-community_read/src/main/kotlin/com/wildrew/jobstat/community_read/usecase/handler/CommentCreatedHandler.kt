package com.wildrew.jobstat.community_read.usecase.handler

import com.wildrew.jobstat.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CommentCreatedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload, Unit>() {
    override val eventType: EventType = EventType.COMMENT_CREATED

    @Transactional
    override fun execute(payload: com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload) {
        communityEventHandler.handleCommentCreated(payload)
    }
}
