package com.example.jobstat.core.config

import com.example.jobstat.core.utils.serializer.DataSerializer
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
    fun dataSerializer(objectMapper: ObjectMapper): DataSerializer {
        return ObjectMapperDataSerializer(objectMapper)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        // iterations는 생성자에서만 설정 가능 (final 필드)
        // 10000 반복은 OWASP 최소 권장값이면서 CPU 사용량 감소
        val encoder =
            Pbkdf2PasswordEncoder(
                "", // 비밀 값 없음
                8, // 솔트 길이 (기본값 16의 절반)
                10000, // 반복 횟수 (기본값 310000의 약 3%)
                SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256, // SHA-256 알고리즘
            )

        // Base64 인코딩 사용 (Hex보다 효율적)
        encoder.setEncodeHashAsBase64(true)

        return encoder
    }
}
