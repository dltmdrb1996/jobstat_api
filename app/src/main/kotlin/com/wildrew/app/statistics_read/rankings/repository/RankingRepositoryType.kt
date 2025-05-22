package com.wildrew.app.statistics_read.rankings.repository

import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RankingRepositoryType(
    val type: RankingType,
)
