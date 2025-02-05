package com.example.jobstat.auth.email.entity

import jakarta.persistence.*
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
    @Column(nullable = false)
    override val email: String,

    @Column(nullable = false, length = 6)
    override val code: String,

    @Column(nullable = false)
    override val expiresAt: LocalDateTime,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long = 0L,
) : ReadEmailVerification {
    override fun isValid() = expiresAt.isAfter(LocalDateTime.now())

    companion object {
        fun create(email: String): EmailVerification {
            val code = generateVerificationCode()
            val expiresAt = LocalDateTime.now().plusMinutes(30)  // 30분 유효
            return EmailVerification(email, code, expiresAt)
        }

        private fun generateVerificationCode(): String =
            Random.nextInt(100000, 999999).toString()
    }
}