package com.example.jobstat.auth.email.service

import org.springframework.scheduling.annotation.Async

// 이메일 서비스 인터페이스
interface EmailService {
    // 비동기로 인증 이메일 발송
    @Async
    fun sendVerificationEmail(
        to: String,
        code: String,
    )
}
