package com.example.jobstat.rankings.fake

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument
import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.base.repository.BaseRankingRepository
import com.example.jobstat.statistics.fake.AbstractFakeTimeSeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class AbstractFakeRankingRepository<T : BaseRankingDocument<E>, E : RankingEntry> :
    AbstractFakeTimeSeriesRepository<T>(),
    BaseRankingRepository<T, E, String> {
    override fun findByPage(
        baseDate: String,
        page: Int,
    ): T =
        documents.values
            .find { it.baseDate == baseDate && it.page == page }
            ?: throw NoSuchElementException("Document not found for baseDate: $baseDate, page: $page")

    override fun findAllPages(baseDate: String): Flow<T> =
        flow {
            documents.values
                .filter { it.baseDate == baseDate }
                .sortedBy { it.page }
                .forEach { emit(it) }
        }

    override fun findTopN(
        baseDate: String,
        limit: Int,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate }
            .flatMap { it.rankings }
            .sortedBy { it.rank }
            .take(limit)

    override fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate }
            .flatMap { it.rankings }
            .filter { it.rank in startRank..endRank }
            .sortedBy { it.rank }

    override fun findByEntityId(
        baseDate: String,
        entityId: Long,
    ): E? =
        documents.values
            .filter { it.baseDate == baseDate }
            .flatMap { it.rankings }
            .find { it.entityId == entityId }

    override fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E> =
        documents.values
            .filter { it.baseDate == endDate }
            .flatMap { it.rankings }
            .filter { it.rankChange != null }
            .sortedByDescending { it.rankChange }
            .take(limit)

    override fun findTopLosers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E> =
        documents.values
            .filter { it.baseDate == endDate }
            .flatMap { it.rankings }
            .filter { it.rankChange != null }
            .sortedBy { it.rankChange }
            .take(limit)
}
