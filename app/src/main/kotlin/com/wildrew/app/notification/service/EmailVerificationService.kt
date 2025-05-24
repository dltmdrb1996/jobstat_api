package com.wildrew.app.notification.service

import com.wildrew.app.notification.entity.ReadEmailVerification

interface EmailVerificationService {
    fun create(email: String): ReadEmailVerification

    fun findLatestByEmail(email: String): ReadEmailVerification?

    fun matchesCode(
        verification: ReadEmailVerification,
        code: String,
    ): Boolean
}
