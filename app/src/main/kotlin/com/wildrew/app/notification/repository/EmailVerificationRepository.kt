package com.wildrew.app.notification.repository

import com.wildrew.app.notification.entity.EmailVerification

interface EmailVerificationRepository {
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
