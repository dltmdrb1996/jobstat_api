package com.example.jobstat.core.security.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val limit: Int = 100,
    val duration: Int = 60
)
