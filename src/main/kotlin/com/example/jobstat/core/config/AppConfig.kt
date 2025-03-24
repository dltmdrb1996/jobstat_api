package com.example.jobstat.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder

@Configuration
class AppConfig {
    @Bean
    @Primary // 우선 순위 지정
    fun passwordEncoder(): PasswordEncoder {
        // 메모리를 많이 사용하고 CPU를 적게 사용하는 Argon2 설정
        val customArgon2Encoder = Argon2PasswordEncoder(
            32,      // saltLength
            64,      // hashLength
            1,       // parallelism - CPU 사용 최소화
            65536,   // memory - 메모리 사용 증가 (64MB)
            1        // iterations - CPU 사용 최소화
        )

        val encoders = HashMap<String, PasswordEncoder>()
        encoders["bcrypt"] = BCryptPasswordEncoder()
        encoders["pbkdf2"] = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        encoders["argon2"] = customArgon2Encoder

        return DelegatingPasswordEncoder("argon2", encoders)
    }
}