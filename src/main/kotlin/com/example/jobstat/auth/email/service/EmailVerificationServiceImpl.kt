package com.example.jobstat.auth.email.service

import com.example.jobstat.auth.email.entity.EmailVerification
import com.example.jobstat.auth.email.entity.ReadEmailVerification
import com.example.jobstat.auth.email.repository.EmailVerificationRepository
import org.springframework.stereotype.Service

@Service
internal class EmailVerificationServiceImpl(
    private val emailVerificationRepository: EmailVerificationRepository,
) : EmailVerificationService {
    override fun create(email: String): EmailVerification =
        // 새 이메일 인증 객체 생성 및 저장
        EmailVerification.create(email).let(emailVerificationRepository::save)

    override fun findLatestByEmail(email: String): EmailVerification? = emailVerificationRepository.findLatestByEmail(email)

    override fun matchesCode(
        verification: ReadEmailVerification,
        code: String,
    ): Boolean = verification.code == code && verification.isValid()
}
