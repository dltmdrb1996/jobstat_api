package com.example.jobstat.statistics.stats.registry

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StatsRepositoryType(
    val type: StatsType,
)
