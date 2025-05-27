package com.wildrew.jobstat.notification.email

import org.springframework.scheduling.annotation.Async

interface EmailService {
    @Async
    fun sendVerificationEmail(
        to: String,
        body: String,
        subject: String,
    )
}
