package com.wildrew.app.community_read.usecase.handler

import com.wildrew.app.community_read.service.CommunityEventHandler
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CommentUpdatedHandler(
    @Qualifier("communityEventHandlerVerLua")
    private val communityEventHandler: CommunityEventHandler,
) : EventHandlingUseCase<EventType, CommentUpdatedEventPayload, Unit>() {
    override val eventType: EventType = EventType.COMMENT_UPDATED

    @Transactional
    override fun execute(payload: CommentUpdatedEventPayload) {
        communityEventHandler.handleCommentUpdated(payload)
    }
}
