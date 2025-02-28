package com.example.jobstat.auth.email.service

import com.example.jobstat.auth.email.entity.ReadEmailVerification

internal interface EmailVerificationService {
    fun create(email: String): ReadEmailVerification

    fun findLatestByEmail(email: String): ReadEmailVerification?

    fun matchesCode(
        verification: ReadEmailVerification,
        code: String,
    ): Boolean
}
