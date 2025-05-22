package com.wildrew.app.statistics_read.core.core_mongo_base.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.RelationshipRankingDocument
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface RelationshipRankingRepository<
    T : RelationshipRankingDocument<E>,
    E : RelationshipRankingDocument.RelationshipRankingEntry,
    ID : Any,
> : BaseRankingRepository<T, E, ID> {
    // 주요 엔티티로 조회
    fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: BaseDate,
    ): E?

    // 주요 엔티티의 관련 엔티티 Top N 조회
    fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<RelationshipRankingDocument.RelatedEntityRank>

    // 관련 엔티티로 조회
    fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: BaseDate,
    ): List<E>

    // 강한 관계를 가진 엔티티들 조회
    fun findStrongRelationships(
        baseDate: BaseDate,
        minScore: Double,
    ): List<E>

    // 가장 강한 관계를 가진 엔티티 쌍 조회
    fun findStrongestPairs(
        baseDate: BaseDate,
        limit: Int,
    ): List<E>

    // 공통 관계를 가진 엔티티들 조회
    fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: BaseDate,
    ): List<RelationshipRankingDocument.RelatedEntityRank>
}

abstract class RelationshipRankingRepositoryImpl<
    T : RelationshipRankingDocument<E>,
    E : RelationshipRankingDocument.RelationshipRankingEntry,
    ID : Any,
>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    RelationshipRankingRepository<T, E, ID> {
    override fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: BaseDate,
    ): E? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.eq("rankings.primary_entity_id", primaryEntityId),
                ),
            ).sort(Sorts.ascending("page"))
            .firstOrNull()
            ?.let { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .find { it.primaryEntityId == primaryEntityId }
            }
    }

    override fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<RelationshipRankingDocument.RelatedEntityRank> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.eq("rankings.primary_entity_id", primaryEntityId),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.primaryEntityId == primaryEntityId }
                    .flatMap { it.relatedRankings }
            }.sortedByDescending { it.score }
            .take(limit)
    }

    override fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: BaseDate,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { entry ->
                        entry.relatedRankings.any { it.entityId == relatedEntityId }
                    }
            }
    }

    override fun findStrongRelationships(
        baseDate: BaseDate,
        minScore: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate.toString()))
            .sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { entry ->
                        entry.relatedRankings.any { it.score >= minScore }
                    }
            }.sortedByDescending { entry ->
                entry.relatedRankings.maxOf { it.score }
            }
    }

    override fun findStrongestPairs(
        baseDate: BaseDate,
        limit: Int,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate.toString()))
            .sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter.read(entityInformation.javaType, doc).rankings
            }.sortedByDescending { entry ->
                entry.relatedRankings.maxOf { it.score }
            }.take(limit)
    }

    override fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: BaseDate,
    ): List<RelationshipRankingDocument.RelatedEntityRank> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val allEntries =
            collection
                .find(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                    ),
                ).sort(Sorts.ascending("page"))
                .flatMap { doc ->
                    mongoOperations.converter.read(entityInformation.javaType, doc).rankings
                }

        val entity1Rankings =
            allEntries
                .find { it.primaryEntityId == primaryEntityId1 }
                ?.relatedRankings
                ?.associateBy { it.entityId }
                ?: emptyMap()

        val entity2Rankings =
            allEntries
                .find { it.primaryEntityId == primaryEntityId2 }
                ?.relatedRankings
                ?.associateBy { it.entityId }
                ?: emptyMap()

        return entity1Rankings.keys
            .intersect(entity2Rankings.keys)
            .mapNotNull { entityId ->
                entity1Rankings[entityId]
            }.sortedByDescending { it.score }
    }
}
