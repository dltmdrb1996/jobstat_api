package com.example.jobstat.statistics_read.core.core_mongo_base.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.example.jobstat.statistics_read.core.core_mongo_base.repository.BaseRankingRepository.Companion.DEFAULT_PAGE_SIZE
import com.example.jobstat.core.core_jpa_base.orThrowNotFound
import com.mongodb.client.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import kotlin.math.abs

@NoRepositoryBean
interface BaseRankingRepository<T : BaseRankingDocument<E>, E : RankingEntry, ID : Any> :
    BaseTimeSeriesRepository<T, ID> {
    // 특정 페이지 조회
    fun findByPage(
        baseDate: String,
        page: Int,
    ): T

    // 전체 페이지 조회
    fun findAllPages(baseDate: String): Flow<T>

    // Top N 조회
    fun findTopN(
        baseDate: String,
        limit: Int,
    ): List<E>

    // 랭크 범위 조회
    fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<E>

    // 특정 엔티티 찾기
    fun findByEntityId(
        baseDate: String,
        entityId: Long,
    ): E?

    // 랭킹 변동이 큰 상위 엔티티 조회
    fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E>

    // 랭킹 하락이 큰 상위 엔티티 조회
    fun findTopLosers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E>

    // 안정적인 랭킹 유지 엔티티 조회
    fun findStableEntities(
        months: Int,
        maxRankChange: Int,
    ): List<E>

    // 변동성 높은 엔티티 조회
    fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<E>

    // 상위 랭킹 지속 유지 엔티티 조회
    fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<E>

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

    // BaseRankingRepositoryImpl.kt의 나머지 메서드들 구현
    override fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        // 모든 페이지에서 rankChange가 존재하는 엔티티들을 찾음
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

        // 최근 N개월간의 데이터를 가져와서 상위 랭킹을 유지한 엔티티들을 찾음
        val recentDocs =
            collection
                .find(Filters.empty())
                .sort(Sorts.descending("base_date"))
                .limit(months)
                .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
                .toList()

        // entityId별로 그룹화하여 모든 기간에서 maxRank 이내였는지 확인
        val consistentEntities =
            recentDocs
                .flatMap { doc -> doc.rankings }
                .groupBy { it.entityId }
                .filter { (_, entries) ->
                    entries.size == months && entries.all { it.rank <= maxRank }
                }.keys

        // 가장 최근 문서에서 해당 엔티티들의 정보를 반환
        return recentDocs
            .firstOrNull()
            ?.rankings
            ?.filter { it.entityId in consistentEntities }
            ?: emptyList()
    }
}
