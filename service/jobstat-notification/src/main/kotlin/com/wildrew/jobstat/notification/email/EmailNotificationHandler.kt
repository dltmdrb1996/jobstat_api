package com.wildrew.jobstat.notification.email // Command 서비스 패키지 구조에 맞게 조정

import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.notification.EmailNotificationEvent
import org.springframework.stereotype.Component

@Component
class EmailNotificationHandler(
    private val emailService: EmailService,
) : EventHandlingUseCase<EventType, EmailNotificationEvent, Unit>() {
    override val eventType: EventType = EventType.EMAIL_NOTIFICATION

    override fun execute(payload: EmailNotificationEvent) {
        log.debug("EMAIL_NOTIFICATION 이벤트 처리 시작: boardId={}", payload.body)

        try {
            emailService.sendVerificationEmail(
                to = payload.to,
                body = payload.body,
                subject = payload.subject,
            )
        } catch (e: Exception) {
            log.error("이메일 발송 중 오류 발생: {}", e.message, e)
            throw e
        }
    }
}
