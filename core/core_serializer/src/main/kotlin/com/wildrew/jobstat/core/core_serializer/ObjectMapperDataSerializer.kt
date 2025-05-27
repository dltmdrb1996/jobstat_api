package com.wildrew.jobstat.core.core_serializer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ObjectMapperDataSerializer(
    private val objectMapper: ObjectMapper,
) : DataSerializer {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun <T : Any> deserialize(
        data: String,
        clazz: Class<T>,
    ): T? =
        try {
            objectMapper.readValue(data, clazz)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.deserialize] ${clazz.simpleName} e = ${e.message}")
            null
        }

    override fun <T : Any> deserialize(
        data: Any,
        clazz: Class<T>,
    ): T? = objectMapper.convertValue(data, clazz)

    override fun serialize(obj: Any): String? =
        try {
            objectMapper.writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.serialize] ${obj.javaClass.simpleName} e = ${e.message}")
            null
        }

    fun <T> deserializeCollection(
        data: String,
        typeReference: TypeReference<T>,
    ): T? =
        try {
            objectMapper.readValue(data, typeReference)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.deserializeCollection] ${typeReference.type} e = ${e.message}")
            null
        }

    fun prettyPrint(obj: Any): String? =
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            log.error("[ObjectMapperDataSerializer.prettyPrint] ${obj.javaClass.simpleName} e = ${e.message}")
            null
        }
}
