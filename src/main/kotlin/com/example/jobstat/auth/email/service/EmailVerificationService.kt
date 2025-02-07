package com.example.jobstat.auth.email.service

import com.example.jobstat.auth.email.entity.ReadEmailVerification

// 이메일 인증 서비스 인터페이스
internal interface EmailVerificationService {
    // 새로운 이메일 인증 생성
    fun create(email: String): ReadEmailVerification

    // 이메일로 최근 인증 정보 조회
    fun findLatestByEmail(email: String): ReadEmailVerification?

    // 인증 코드 일치 여부 확인
    fun matchesCode(
        verification: ReadEmailVerification,
        code: String,
    ): Boolean
}
