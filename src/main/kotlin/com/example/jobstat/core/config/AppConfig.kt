package com.example.jobstat.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm

@Configuration
class AppConfig {
    /**
     * CPU 리소스를 절약하기 위한 PBKDF2 암호화 설정
     * - 알고리즘: PBKDF2WithHmacSHA256 (보안성과 효율성의 균형)
     * - 반복 횟수: 10000 (기본값 310000보다 대폭 감소)
     * - 솔트 길이: 8 (기본값 16보다 감소)
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        // iterations는 생성자에서만 설정 가능 (final 필드)
        // 10000 반복은 OWASP 최소 권장값이면서 CPU 사용량 감소
        val encoder = Pbkdf2PasswordEncoder(
            "", // 비밀 값 없음
            8,  // 솔트 길이 (기본값 16의 절반)
            10000, // 반복 횟수 (기본값 310000의 약 3%)
            SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256 // SHA-256 알고리즘
        )

        // Base64 인코딩 사용 (Hex보다 효율적)
        encoder.setEncodeHashAsBase64(true)

        return encoder
    }
}