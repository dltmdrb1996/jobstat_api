package com.example.jobstat.core.global.utils.serializer

import kotlin.reflect.KClass

/**
 * 데이터 직렬화/역직렬화를 위한 인터페이스
 */
interface DataSerializer {
    /**
     * 문자열 데이터를 지정한 클래스 타입으로 역직렬화
     */
    fun <T : Any> deserialize(data: String, clazz: Class<T>): T?

    /**
     * 객체를 다른 클래스 타입으로 변환
     */
    fun <T : Any> deserialize(data: Any, clazz: Class<T>): T?

    /**
     * KClass를 사용한 직렬화 (Kotlin 스타일)
     */
    fun <T : Any> deserialize(data: String, kClass: KClass<T>): T? {
        return deserialize(data, kClass.java)
    }

    /**
     * KClass를 사용한 변환 (Kotlin 스타일)
     */
    fun <T : Any> deserialize(data: Any, kClass: KClass<T>): T? {
        return deserialize(data, kClass.java)
    }

    /**
     * 객체를 JSON 문자열로 직렬화
     */
    fun serialize(obj: Any): String?
}

/**
 * 인라인 함수를 사용한 타입 추론 역직렬화
 */
inline fun <reified T : Any> DataSerializer.deserialize(data: String): T? {
    return deserialize(data, T::class)
}

/**
 * String 확장 함수: JSON 문자열을 지정한 타입으로 역직렬화
 */
inline fun <reified T : Any> String.deserializeAs(dataSerializer: DataSerializer): T? {
    return dataSerializer.deserialize(this, T::class)
}
