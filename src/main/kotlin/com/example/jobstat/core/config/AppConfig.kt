package com.example.jobstat.core.config

import com.example.jobstat.core.global.utils.ScopedValueSecurityUtils
import com.example.jobstat.core.global.utils.SecurityUtils
import com.example.jobstat.core.global.utils.ThreadLocalSecurityUtils
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import com.example.jobstat.core.global.utils.serializer.ObjectMapperDataSerializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm

@Configuration
class AppConfig {
    companion object {
        val OBJECT_MAPPER: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(JavaTimeModule())
                .addModule(AfterburnerModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)
                .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .build()
                .registerKotlinModule()
    }

    @Bean
    fun objectMapper(): ObjectMapper = OBJECT_MAPPER

    @Bean
    fun dataSerializer(objectMapper: ObjectMapper): DataSerializer = ObjectMapperDataSerializer(objectMapper)

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        val encoder =
            Pbkdf2PasswordEncoder(
                "",
                8,
                10000,
                SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256,
            )

        encoder.setEncodeHashAsBase64(true)
        return encoder
    }

    @Bean
    @Primary
    fun primarySecurityOperations(): SecurityUtils {
        return ScopedValueSecurityUtils()
    }
}
