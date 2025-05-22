package com.wildrew.app.auth.email.service

import org.springframework.scheduling.annotation.Async

interface EmailService {
    @Async
    fun sendVerificationEmail(
        to: String,
        code: String,
    )
}
