package com.example.jobstat.core.core_security.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiVersion(
    val value: Int = 1,
)
