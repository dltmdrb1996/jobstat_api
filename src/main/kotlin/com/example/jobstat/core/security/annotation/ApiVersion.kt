package com.example.jobstat.core.security.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiVersion(
    val value: Int = 1
)