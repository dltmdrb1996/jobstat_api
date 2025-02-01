package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.mongo.ranking.RankingType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RankingRepositoryType(
    val type: RankingType,
)
