package com.example.jobstat.core.core_config

import com.example.jobstat.core.core_security.util.ScopedValueSecurityUtils
import com.example.jobstat.core.core_security.util.SecurityUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class AppConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        val encoder =
            Pbkdf2PasswordEncoder(
                "",
                8,
                10000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256,
            )

        encoder.setEncodeHashAsBase64(true)
        return encoder
    }

    @Bean
    @Primary
    fun primarySecurityOperations(): SecurityUtils {
        return ScopedValueSecurityUtils()
    }

    @Bean(destroyMethod = "close")
    fun virtualThreadExecutor(): ExecutorService =
        Executors.newVirtualThreadPerTaskExecutor()

    @Bean
    fun virtualThreadDispatcher(
        virtualThreadExecutor: ExecutorService
    ): CoroutineDispatcher =
        virtualThreadExecutor.asCoroutineDispatcher()

    @Bean
    fun virtualThreadScope(
        virtualThreadDispatcher: CoroutineDispatcher
    ): CoroutineScope =
        CoroutineScope(virtualThreadDispatcher + SupervisorJob())
}