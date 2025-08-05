package com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.common.orThrowNotFound
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.RankingSlice
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseRankingRepository.Companion.DEFAULT_PAGE_SIZE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import kotlin.math.abs

@NoRepositoryBean
interface BaseRankingRepository<T : BaseRankingDocument<E>, E : RankingEntry, ID : Any> : BaseTimeSeriesRepository<T, ID> {
    fun findByPage(
        baseDate: String,
        page: Int,
    ): T

    fun findByPageRange(
        baseDate: String,
        startPage: Int,
        endPage: Int,
    ): List<T>

    fun findAllPages(baseDate: String): Flow<T>

    fun findTopN(
        baseDate: String,
        limit: Int,
    ): List<E>

    fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<E>

    fun findByEntityId(
        baseDate: String,
        entityId: Long,
    ): E?

    fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E>

    fun findTopLosers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E>

    fun findStableEntities(
        months: Int,
        maxRankChange: Int,
    ): List<E>

    fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<E>

    fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<E>

    fun findRankingsSlice(baseDate: String, cursor: Int?, limit: Int): RankingSlice<E>

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
    }
}

abstract class BaseRankingRepositoryImpl<T : BaseRankingDocument<E>, E : RankingEntry, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseTimeSeriesRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    BaseRankingRepository<T, E, ID> {
    override fun findByPage(
        baseDate: String,
        page: Int,
    ): T {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.eq("page", page),
                ),
            ).firstOrNull()
            .orThrowNotFound(entityInformation.collectionName, "baseDate: $baseDate, page: $page")
            .let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByPageRange(
        baseDate: String,
        startPage: Int,
        endPage: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.gte("page", startPage),
                    Filters.lte("page", endPage),
                ),
            ).sort(Sorts.ascending("page"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findAllPages(baseDate: String): Flow<T> =
        flow {
            val collection = mongoOperations.getCollection(entityInformation.collectionName)

            collection
                .find(Filters.eq("base_date", baseDate))
                .sort(Sorts.ascending("page"))
                .forEach { doc ->
                    emit(mongoOperations.converter.read(entityInformation.javaType, doc))
                }
        }

    override fun findTopN(
        baseDate: String,
        limit: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val requiredPages = (limit + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE

        return collection
            .find(Filters.eq("base_date", baseDate))
            .sort(Sorts.ascending("page"))
            .limit(requiredPages)
            .flatMap { doc ->
                mongoOperations.converter.read(entityInformation.javaType, doc).rankings
            }.take(limit)
    }

    override fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val startPage = (startRank - 1) / DEFAULT_PAGE_SIZE + 1
        val endPage = (endRank - 1) / DEFAULT_PAGE_SIZE + 1

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.gte("page", startPage),
                    Filters.lte("page", endPage),
                ),
            ).sort(Sorts.ascending("page"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .flatMap { doc ->
                doc.rankings.filter { it.rank in startRank..endRank }
            }
    }

    override fun findByEntityId(
        baseDate: String,
        entityId: Long,
    ): E? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate))
            .mapNotNull { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .find { it.entityId == entityId }
            }.firstOrNull()
    }

    override fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", endDate))
            .sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.rankChange != null }
            }.sortedByDescending { it.rankChange }
            .take(limit)
    }

    override fun findTopLosers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", endDate))
            .sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.rankChange != null }
            }.sortedBy { it.rankChange }
            .take(limit)
    }

    override fun findStableEntities(
        months: Int,
        maxRankChange: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.empty())
            .sort(Sorts.descending("base_date"))
            .limit(months)
            .flatMap { doc ->
                val document = mongoOperations.converter.read(entityInformation.javaType, doc)
                document.rankings.filter { entry ->
                    val rankChange = entry.rankChange
                    rankChange == null || abs(rankChange) <= maxRankChange
                }
            }
    }

    override fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.empty())
            .sort(Sorts.descending("base_date"))
            .limit(months)
            .flatMap { doc ->
                val document = mongoOperations.converter.read(entityInformation.javaType, doc)
                document.rankings.filter { entry ->
                    val rankChange = entry.rankChange
                    rankChange != null && abs(rankChange) >= minRankChange
                }
            }
    }

    override fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val recentDocs =
            collection
                .find(Filters.empty())
                .sort(Sorts.descending("base_date"))
                .limit(months)
                .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
                .toList()

        val consistentEntities =
            recentDocs
                .flatMap { doc -> doc.rankings }
                .groupBy { it.entityId }
                .filter { (_, entries) ->
                    entries.size == months && entries.all { it.rank <= maxRank }
                }.keys

        return recentDocs
            .firstOrNull()
            ?.rankings
            ?.filter { it.entityId in consistentEntities }
            ?: emptyList()
    }

    override fun findRankingsSlice(baseDate: String, cursor: Int?, limit: Int): RankingSlice<E> {
        val startIndex = cursor ?: 0
        val query = Query(Criteria.where("base_date").`is`(baseDate))
        query.fields()
            .include("_id", "base_date", "period", "metrics", "page")
            .slice("rankings", startIndex, limit)

        val document = mongoOperations.findOne(query, entityInformation.javaType)
            ?: return RankingSlice()

        return RankingSlice(
            totalCount = document.metrics.rankedCount,
            items = document.rankings
        )
    }
}
