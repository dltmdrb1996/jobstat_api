package com.example.jobstat.core.security.annotation

/**
 * 요청 속도 제한을 설정하는 어노테이션
 * @param limit 허용되는 최대 요청 수
 * @param duration 제한 시간(초)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val limit: Int = 100,
    val duration: Int = 60,
)
