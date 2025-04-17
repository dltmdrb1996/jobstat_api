package com.example.jobstat.core.global.utils.serializer

import kotlin.reflect.KClass

interface DataSerializer {
    fun <T : Any> deserialize(
        data: String,
        clazz: Class<T>,
    ): T?

    fun <T : Any> deserialize(
        data: Any,
        clazz: Class<T>,
    ): T?

    fun <T : Any> deserialize(
        data: String,
        kClass: KClass<T>,
    ): T? = deserialize(data, kClass.java)

    fun <T : Any> deserialize(
        data: Any,
        kClass: KClass<T>,
    ): T? = deserialize(data, kClass.java)

    fun serialize(obj: Any): String?
}
