package com.wildrew.jobstat.core.core_serializer

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
