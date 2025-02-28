// EmailVerificationRepository.kt (도메인 레포지토리 인터페이스)
package com.example.jobstat.auth.email.repository

import com.example.jobstat.auth.email.entity.EmailVerification

// 이메일 인증 레포지토리 인터페이스
internal interface EmailVerificationRepository {
    fun save(emailVerification: EmailVerification): EmailVerification

    fun findById(id: Long): EmailVerification

    // 이메일로 최근 인증 정보 조회
    fun findLatestByEmail(email: String): EmailVerification?

    // 이메일과 인증 코드 일치 여부 확인
    fun existsByEmailAndCode(
        email: String,
        code: String,
    ): Boolean

    fun delete(emailVerification: EmailVerification)

    // 만료된 인증 정보 삭제
    fun deleteExpired()
}
