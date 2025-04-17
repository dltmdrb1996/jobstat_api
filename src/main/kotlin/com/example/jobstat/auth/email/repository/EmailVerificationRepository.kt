package com.example.jobstat.auth.email.repository

import com.example.jobstat.auth.email.entity.EmailVerification

internal interface EmailVerificationRepository {
    fun save(emailVerification: EmailVerification): EmailVerification

    fun findById(id: Long): EmailVerification

    fun findLatestByEmail(email: String): EmailVerification?

    fun existsByEmailAndCode(
        email: String,
        code: String,
    ): Boolean

    fun delete(emailVerification: EmailVerification)

    fun deleteExpired()
}
