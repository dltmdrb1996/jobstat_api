package com.wildrew.app.auth.email.service

import com.wildrew.app.auth.email.entity.EmailVerification
import com.wildrew.app.auth.email.entity.ReadEmailVerification
import com.wildrew.app.auth.email.repository.EmailVerificationRepository
import org.springframework.stereotype.Service

@Service
class EmailVerificationServiceImpl(
    private val emailVerificationRepository: EmailVerificationRepository,
) : EmailVerificationService {
    override fun create(email: String): EmailVerification = EmailVerification.create(email).let(emailVerificationRepository::save)

    override fun findLatestByEmail(email: String): EmailVerification? = emailVerificationRepository.findLatestByEmail(email)

    override fun matchesCode(
        verification: ReadEmailVerification,
        code: String,
    ): Boolean = verification.code == code && verification.isValid()
}
