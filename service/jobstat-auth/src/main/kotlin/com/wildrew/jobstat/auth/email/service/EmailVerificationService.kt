package com.wildrew.jobstat.auth.email.service

import com.wildrew.jobstat.auth.email.entity.ReadEmailVerification

interface EmailVerificationService {
    fun create(email: String): ReadEmailVerification

    fun findLatestByEmail(email: String): ReadEmailVerification?

    fun matchesCode(
        verification: ReadEmailVerification,
        code: String,
    ): Boolean
}
