package com.example.jobstat.auth.email.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import kotlin.random.Random

// 이메일 인증 정보 읽기 인터페이스
interface ReadEmailVerification {
    val id: Long
    val email: String
    val code: String
    val expiresAt: LocalDateTime

    fun isValid(): Boolean
}

// 이메일 인증 엔티티
@Entity
@Table(name = "email_verifications")
class EmailVerification(
    email: String,
    code: String,
    expiresAt: LocalDateTime,
) : BaseEntity(),
    ReadEmailVerification {
    @Column(nullable = false)
    override val email: String = email

    @Column(nullable = false, length = 6)
    override val code: String = code

    @Column(nullable = false)
    override val expiresAt: LocalDateTime = expiresAt

    // 인증 코드 유효성 검사
    override fun isValid() = expiresAt.isAfter(LocalDateTime.now())

    companion object {
        // 새로운 이메일 인증 생성
        fun create(email: String): EmailVerification {
            val code = generateVerificationCode()
            val expiresAt = LocalDateTime.now().plusMinutes(30) // 30분 유효기간
            return EmailVerification(email, code, expiresAt)
        }

        // 6자리 인증 코드 생성
        private fun generateVerificationCode(): String = Random.nextInt(100000, 999999).toString()
    }
}
