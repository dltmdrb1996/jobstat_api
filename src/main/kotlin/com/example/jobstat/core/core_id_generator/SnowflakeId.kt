package com.example.jobstat.core.core_id_generator // 또는 적절한 위치

import org.hibernate.annotations.IdGeneratorType
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Snowflake ID 생성을 위한 커스텀 어노테이션.
 * 이 어노테이션 자체가 ID 생성을 의미하며, SnowflakeIdGenerator와 연결된다.
 */
@IdGeneratorType(SnowflakeIdGenerator::class) // 1. 사용할 Generator 클래스 지정
@Retention(AnnotationRetention.RUNTIME) // 2. 런타임 유지 정책 필수
@Target(FIELD, FUNCTION) // 3. 필드 또는 메서드(Getter)에 적용 가능
annotation class SnowflakeId
