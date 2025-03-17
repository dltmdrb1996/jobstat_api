package com.example.jobstat.core.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperConfig {
    companion object {
        val OBJECT_MAPPER: ObjectMapper =
            ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerKotlinModule()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 성능 최적화 설정 추가
                .configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                // 불필요한 기능 비활성화
                .configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
                .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
    }

    @Bean
    fun objectMapper(): ObjectMapper = OBJECT_MAPPER
}