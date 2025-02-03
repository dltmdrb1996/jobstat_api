package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface SimpleRankingRepository<T : SimpleRankingDocument<E>, E : SimpleRankingDocument.SimpleRankingEntry, ID : Any> : BaseRankingRepository<T, E, ID> {
    // Value(score) 범위 기반 검색
    fun findByValueRange(
        baseDate: String,
        minValue: Double,
        maxValue: Double,
    ): List<E>

    // Rank 변화량 기반 Rising Stars 검색
    fun findRisingStars(
        months: Int,
        minRankImprovement: Int,
    ): List<E>

    // 특정 baseDate의 entityId로 조회
    fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T?
}

abstract class SimpleRankingRepositoryImpl<T : SimpleRankingDocument<E>, E : SimpleRankingDocument.SimpleRankingEntry, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    SimpleRankingRepository<T, E, ID> {
    override fun findByValueRange(
        baseDate: String,
        minValue: Double,
        maxValue: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate))
            .sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.score in minValue..maxValue }
            }.sortedByDescending { it.score }
    }

    override fun findRisingStars(
        months: Int,
        minRankImprovement: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.empty())
            .sort(Sorts.descending("base_date"))
            .limit(months)
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { entry ->
                        entry.rankChange != null && (entry.rankChange ?: 0) >= minRankImprovement
                    }
            }.sortedByDescending { it.rankChange }
    }

    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.elemMatch(
                        "rankings",
                        Filters.eq("entity_id", entityId),
                    ),
                ),
            ).firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }
}
