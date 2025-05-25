package com.wildrew.app.auth

import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.notification.EmailNotificationEvent
import com.wildrew.jobstat.core.core_event.outbox.OutboxEventPublisher
import com.wildrew.jobstat.core.core_event.publisher.AbstractEventPublisher
import org.springframework.stereotype.Component

@Component
class UserEventPublisher(
    outboxEventPublisher: OutboxEventPublisher,
) : AbstractEventPublisher(outboxEventPublisher) {
    /**
     * 이 퍼블리셔가 지원하는 이벤트 타입들
     */
    override fun getSupportedEventTypes(): Set<EventType> = SUPPORTED_EVENT_TYPES

    companion object {
        private val SUPPORTED_EVENT_TYPES =
            setOf(
                EventType.EMAIL_NOTIFICATION,
            )
    }

    fun publishEmailNotification(
        to: String,
        subject: String,
        body: String,
    ) {
        val payload =
            EmailNotificationEvent(
                to = to,
                subject = subject,
                body = body,
            )
        publish(EventType.EMAIL_NOTIFICATION, payload)
    }
}
