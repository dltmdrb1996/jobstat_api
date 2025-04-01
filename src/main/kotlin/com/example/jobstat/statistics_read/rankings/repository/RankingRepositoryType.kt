package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RankingRepositoryType(
    val type: RankingType,
)
