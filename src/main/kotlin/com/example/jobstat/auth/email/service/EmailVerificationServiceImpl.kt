package com.example.jobstat.auth.email.service

import com.example.jobstat.auth.email.entity.EmailVerification
import com.example.jobstat.auth.email.entity.ReadEmailVerification
import com.example.jobstat.auth.email.repository.EmailVerificationRepository
import org.springframework.stereotype.Service

@Service
internal class EmailVerificationServiceImpl(
    private val emailVerificationRepository: EmailVerificationRepository,
) : EmailVerificationService {

    override fun create(email: String): EmailVerification {
        val verification = EmailVerification.create(email)
        return emailVerificationRepository.save(verification)
    }

    override fun findLatestByEmail(email: String): EmailVerification? {
        return emailVerificationRepository.findLatestByEmail(email)
    }

    override fun matchesCode(verification: ReadEmailVerification, code: String): Boolean {
        return verification.code == code && verification.isValid()
    }
}