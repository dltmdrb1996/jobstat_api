package com.wildrew.jobstat.core.core_serializer.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import com.wildrew.jobstat.core.core_serializer.ObjectMapperDataSerializer
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
        fun createDefaultObjectMapper(): ObjectMapper =
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

    @Bean("coreObjectMapper")
    @Primary
    @ConditionalOnMissingBean(ObjectMapper::class)
    fun coreObjectMapper(): ObjectMapper {
        log.info("Configuring CoreObjectMapper bean with default settings.")
        return createDefaultObjectMapper()
    }

    @Bean("coreDataSerializer")
    @Primary
    @ConditionalOnMissingBean(DataSerializer::class)
    fun coreDataSerializer(
        objectMapper: ObjectMapper,
    ): DataSerializer {
        log.info("Configuring CoreDataSerializer bean using ObjectMapper: {}", objectMapper.javaClass.simpleName)
        return ObjectMapperDataSerializer(objectMapper)
    }
}
