package com.example.jobstat.utils.base

import com.example.jobstat.core.base.mongo.ranking.RelationshipRankingDocument
import com.example.jobstat.core.base.mongo.ranking.RelationshipRankingDocument.RelatedEntityRank
import com.example.jobstat.core.base.repository.RelationshipRankingRepository
import com.example.jobstat.core.state.BaseDate

abstract class AbstractFakeRelationshipRankingRepository<
    T : RelationshipRankingDocument<E>,
    E : RelationshipRankingDocument.RelationshipRankingEntry,
> :
    AbstractFakeRankingRepository<T, E>(),
    RelationshipRankingRepository<T, E, String> {
    override fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: BaseDate,
    ): E? =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .find { it.primaryEntityId == primaryEntityId }

    override fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<RelatedEntityRank> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { it.primaryEntityId == primaryEntityId }
            .flatMap { it.relatedRankings }
            .sortedByDescending { it.score }
            .take(limit)

    override fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: BaseDate,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { entry ->
                entry.relatedRankings.any { it.entityId == relatedEntityId }
            }

    override fun findStrongRelationships(
        baseDate: BaseDate,
        minScore: Double,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { entry ->
                entry.relatedRankings.any { it.score >= minScore }
            }.sortedByDescending { entry ->
                entry.relatedRankings.maxOf { it.score }
            }

    override fun findStrongestPairs(
        baseDate: BaseDate,
        limit: Int,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .sortedByDescending { entry ->
                entry.relatedRankings.maxOf { it.score }
            }.take(limit)

    override fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: BaseDate,
    ): List<RelatedEntityRank> {
        val allEntries =
            documents.values
                .filter { it.baseDate == baseDate.toString() }
                .flatMap { it.rankings }

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
