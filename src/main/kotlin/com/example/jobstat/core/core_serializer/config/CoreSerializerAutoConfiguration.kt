package com.example.jobstat.core.core_serializer.config

import com.example.jobstat.core.core_serializer.DataSerializer
import com.example.jobstat.core.core_serializer.ObjectMapperDataSerializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@AutoConfiguration
@ConditionalOnClass(ObjectMapper::class)
class CoreSerializerAutoConfiguration {

    private val log = LoggerFactory.getLogger(CoreSerializerAutoConfiguration::class.java)

    companion object {
        // 라이브러리가 기본으로 제공할 ObjectMapper 설정을 정의
        // 이 인스턴스를 직접 빈으로 등록하거나, @Bean 메소드 내에서 생성 가능
        fun createDefaultObjectMapper(): ObjectMapper {
            return JsonMapper
                .builder()
                .addModule(JavaTimeModule()) // Java 8 날짜/시간 타입 지원
                .addModule(AfterburnerModule()) // Jackson 성능 향상 (선택적 의존성)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 날짜를 ISO 문자열로
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 알 수 없는 JSON 필드 무시
                .disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)
                .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES) // 무시된 프로퍼티 에러 방지
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS) // Getter를 Setter처럼 사용하는 것 방지
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS) // isXXX 형태의 Getter 자동 감지 비활성화
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER) // JPA @Transient 필드 등을 Jackson도 무시하도록
                .build()
                .registerKotlinModule() // Kotlin 지원 모듈 등록
        }
    }

    @Bean("coreObjectMapper")
    @Primary
    @ConditionalOnMissingBean(ObjectMapper::class)
    fun coreObjectMapper(): ObjectMapper {
        log.info("Configuring CoreObjectMapper bean with default settings.")
        return createDefaultObjectMapper()
    }

    @Bean("coreDataSerializer") // 빈 이름 명시
    @Primary
    @ConditionalOnMissingBean(DataSerializer::class)
    fun coreDataSerializer(
        objectMapper: ObjectMapper
    ): DataSerializer {
        log.info("Configuring CoreDataSerializer bean using ObjectMapper: {}", objectMapper.javaClass.simpleName)
        return ObjectMapperDataSerializer(objectMapper)
    }
}