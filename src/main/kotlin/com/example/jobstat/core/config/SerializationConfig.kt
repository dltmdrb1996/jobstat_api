package com.example.jobstat.core.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

@Configuration
class SerializationConfig {
    
    @Bean
    fun kotlinSerializationConverter(): KotlinSerializationJsonHttpMessageConverter {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
            isLenient = true
        }
        return KotlinSerializationJsonHttpMessageConverter(json)
    }
    
    @Bean
    fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // Kotlin Serialization 컨버터를 Jackson보다 우선 순위에 둠
        converters.add(0, kotlinSerializationConverter())
    }
}