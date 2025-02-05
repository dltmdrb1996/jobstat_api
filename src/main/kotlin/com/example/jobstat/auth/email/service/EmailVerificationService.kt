package com.example.jobstat.auth.email.service

import com.example.jobstat.auth.email.repository.EmailVerificationRepository
import com.example.jobstat.auth.email.entity.EmailVerification
import com.example.jobstat.auth.email.entity.ReadEmailVerification
import org.springframework.stereotype.Service

internal interface EmailVerificationService {
    fun create(email: String): ReadEmailVerification
    fun findLatestByEmail(email: String): ReadEmailVerification?
    fun matchesCode(verification: ReadEmailVerification, code: String): Boolean
}

