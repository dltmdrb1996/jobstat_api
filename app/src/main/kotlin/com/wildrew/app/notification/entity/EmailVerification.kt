package com.wildrew.app.notification.entity

import com.wildrew.jobstat.core.core_jpa_base.base.AuditableEntitySnow
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime
import kotlin.random.Random

interface ReadEmailVerification {
    val id: Long
    val email: String
    val code: String
    val expiresAt: LocalDateTime

    fun isValid(): Boolean
}

@Entity
@Table(name = "email_verifications")
class EmailVerification(
    email: String,
    code: String,
    expiresAt: LocalDateTime,
) : AuditableEntitySnow(),
    ReadEmailVerification {
    @Column(nullable = false)
    override val email: String = email

    @Column(nullable = false, length = VERIFICATION_CODE_LENGTH)
    override val code: String = code

    @Column(nullable = false)
    override val expiresAt: LocalDateTime = expiresAt

    override fun isValid() = expiresAt.isAfter(LocalDateTime.now())

    companion object {
        private const val VERIFICATION_CODE_LENGTH = 6
        private const val EXPIRATION_MINUTES = 30L
        private const val MIN_CODE_VALUE = 100000
        private const val MAX_CODE_VALUE = 999999

        fun create(email: String): EmailVerification {
            val code = generateVerificationCode()
            val expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES)
            return EmailVerification(email, code, expiresAt)
        }

        private fun generateVerificationCode(): String = Random.nextInt(MIN_CODE_VALUE, MAX_CODE_VALUE).toString()
    }
}
