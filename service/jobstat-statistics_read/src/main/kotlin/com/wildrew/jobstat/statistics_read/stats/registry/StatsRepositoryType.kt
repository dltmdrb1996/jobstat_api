package com.wildrew.jobstat.statistics_read.stats.registry

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StatsRepositoryType(
    val type: StatsType,
)
