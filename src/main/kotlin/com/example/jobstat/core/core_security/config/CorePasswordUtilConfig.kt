package com.example.jobstat.core.core_security.config

import com.example.jobstat.core.core_security.util.DefaultPasswordUtil
import com.example.jobstat.core.core_security.util.PasswordUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder

@AutoConfiguration
class CorePasswordUtilConfig {

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder::class)
    fun corePasswordEncoder( // Bean 이름을 `corePasswordEncoder` 등으로 하여 충돌 방지
        @Value("\${jobstat.core.security.password-encoder.salt:}") salt: String, // 프로퍼티로 salt 주입 (비어있을 수 있음)
        @Value("\${jobstat.core.security.password-encoder.iterations:10000}") iterations: Int,
        @Value("\${jobstat.core.security.password-encoder.secret-key-factory-algorithm:PBKDF2WithHmacSHA256}") algorithm: String
    ): PasswordEncoder {
        val encoder =
            Pbkdf2PasswordEncoder(
                salt, // 프로퍼티로 주입받은 salt 사용
                8, // PBKDF2WithHmacSHA256의 기본 salt length는 8 byte
                iterations,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.valueOf(algorithm.replace("-", "_")) // Enum 변환
            )
        encoder.setEncodeHashAsBase64(true)
        return encoder
    }

    @Bean
    @ConditionalOnMissingBean(PasswordUtil::class)
    fun corePasswordUtil(passwordEncoder: PasswordEncoder): PasswordUtil {
        return DefaultPasswordUtil(passwordEncoder)
    }
}