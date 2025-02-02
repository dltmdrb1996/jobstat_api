package com.example.jobstat.statistics.fake

import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.utils.base.AbstractFakeRankingRepository

abstract class AbstractFakeSimpleRankingRepository<T : SimpleRankingDocument<E>, E : SimpleRankingDocument.SimpleRankingEntry> :
    AbstractFakeRankingRepository<T, E>(),
    SimpleRankingRepository<T, E, String> {
    override fun findByValueRange(
        baseDate: String,
        minValue: Double,
        maxValue: Double,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate }
            .flatMap { it.rankings }
            .filter { it.score in minValue..maxValue }
            .sortedByDescending { it.score }

    override fun findRisingStars(
        months: Int,
        minRankImprovement: Int,
    ): List<E> =
        documents.values
            .sortedByDescending { it.baseDate }
            .take(months)
            .flatMap { it.rankings }
            .filter { it.rankChange != null && (it.rankChange ?: 0) >= minRankImprovement }
            .sortedByDescending { it.rankChange }

    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T? =
        documents.values
            .find { doc ->
                doc.baseDate == baseDate && doc.rankings.any { it.entityId == entityId }
            }
}
