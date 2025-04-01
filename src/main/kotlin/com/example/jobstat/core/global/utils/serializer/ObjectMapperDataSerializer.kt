// ObjectMapperDataSerializer.kt
package com.example.jobstat.core.global.utils.serializer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ObjectMapperDataSerializer(
    private val objectMapper: ObjectMapper
) : DataSerializer {

    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun <T : Any> deserialize(data: String, clazz: Class<T>): T? {
        return try {
            objectMapper.readValue(data, clazz)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.deserialize] ${clazz.simpleName} e = ${e.message}",)
            null
        }
    }

    override fun <T : Any> deserialize(data: Any, clazz: Class<T>): T {
        return objectMapper.convertValue(data, clazz)
    }

    override fun serialize(obj: Any): String? {
        return try {
            objectMapper.writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.serialize] ${obj.javaClass.simpleName} e = ${e.message}")
            null
        }
    }

    // 추가 편의 기능 - 컬렉션 타입 역직렬화를 위한 메서드
    fun <T> deserializeCollection(data: String, typeReference: TypeReference<T>): T? {
        return try {
            objectMapper.readValue(data, typeReference)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.deserializeCollection] ${typeReference.type} e = ${e.message}")
            null
        }
    }

    // prettyPrint 기능 제공
    fun prettyPrint(obj: Any): String? {
        return try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.prettyPrint] ${obj.javaClass.simpleName} e = ${e.message}")
            null
        }
    }
}

// TypeReference 관련 확장 함수
inline fun <reified T> DataSerializer.deserializeCollection(data: String): T? {
    return if (this is ObjectMapperDataSerializer) {
        this.deserializeCollection(data, object : TypeReference<T>() {})
    } else {
        null
    }
}

// 문자열용 확장 함수
inline fun <reified T> String.deserializeCollectionAs(dataSerializer: DataSerializer): T? {
    return dataSerializer.deserializeCollection<T>(this)
}