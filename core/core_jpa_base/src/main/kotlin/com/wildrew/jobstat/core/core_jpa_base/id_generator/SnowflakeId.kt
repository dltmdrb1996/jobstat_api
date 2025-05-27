package com.wildrew.jobstat.core.core_jpa_base.id_generator

import org.hibernate.annotations.IdGeneratorType
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Snowflake ID 생성을 위한 커스텀 어노테이션.
 * 이 어노테이션 자체가 ID 생성을 의미하며, SnowflakeIdGenerator와 연결된다.
 */
@IdGeneratorType(SnowflakeIdGenerator::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(FIELD, FUNCTION)
annotation class SnowflakeId
