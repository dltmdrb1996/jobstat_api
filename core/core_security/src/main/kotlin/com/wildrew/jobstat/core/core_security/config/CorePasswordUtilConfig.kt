package com.wildrew.jobstat.core.core_security.config

import com.wildrew.jobstat.core.core_security.util.DefaultPasswordUtil
import com.wildrew.jobstat.core.core_security.util.PasswordUtil
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
    fun corePasswordEncoder(
        @Value("\${jobstat.core.security.password-encoder.salt:}") salt: String,
        @Value("\${jobstat.core.security.password-encoder.iterations:10000}") iterations: Int,
        @Value("\${jobstat.core.security.password-encoder.secret-key-factory-algorithm:PBKDF2WithHmacSHA256}") algorithm: String,
    ): PasswordEncoder {
        val encoder =
            Pbkdf2PasswordEncoder(
                salt,
                8,
                iterations,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.valueOf(algorithm.replace("-", "_")),
            )
        encoder.setEncodeHashAsBase64(true)
        return encoder
    }

    @Bean
    @ConditionalOnMissingBean(PasswordUtil::class)
    fun corePasswordUtil(passwordEncoder: PasswordEncoder): PasswordUtil = DefaultPasswordUtil(passwordEncoder)
}
