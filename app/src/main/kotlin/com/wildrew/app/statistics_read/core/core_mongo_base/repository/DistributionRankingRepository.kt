package com.wildrew.app.statistics_read.core.core_mongo_base.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.DistributionRankingDocument
import com.wildrew.jobstat.core.core_global.model.BaseDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface DistributionRankingRepository<
    T : DistributionRankingDocument<E>,
    E : DistributionRankingDocument.DistributionRankingEntry,
    ID : Any,
> : BaseRankingRepository<T, E, ID> {
    fun findByDistributionPattern(
        baseDate: BaseDate,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<E>

    fun findByDominantCategory(
        baseDate: BaseDate,
        category: String,
    ): List<E>

    fun findDistributionTrends(
        entityId: Long,
        months: Int,
    ): List<T>

    fun findSignificantDistributionChanges(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<E>

    fun findSimilarDistributions(
        entityId: Long,
        baseDate: BaseDate,
        similarity: Double,
    ): List<E>

    fun findUniformDistributions(
        baseDate: BaseDate,
        maxVariance: Double,
    ): List<E>

    fun findSkewedDistributions(
        baseDate: BaseDate,
        minSkewness: Double,
    ): List<E>

    fun findDistributionChanges(
        entityId: Long,
        months: Int,
    ): List<T>

    fun findCategoryDominance(
        baseDate: BaseDate,
        category: String,
        minPercentage: Double,
    ): List<E>
}

abstract class DistributionRankingRepositoryImpl<
    T : DistributionRankingDocument<E>,
    E : DistributionRankingDocument.DistributionRankingEntry,
    ID : Any,
>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    DistributionRankingRepository<T, E, ID> {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun findByDistributionPattern(
        baseDate: BaseDate,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate.toString()))
            .sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter.read(entityInformation.javaType, doc).rankings.filter { entry ->
                    val similarity = calculateSimilarity(entry.distribution, pattern)
                    similarity >= threshold
                }
            }.sortedByDescending { entry ->
                calculateSimilarity(entry.distribution, pattern)
            }
    }

    override fun findByDominantCategory(
        baseDate: BaseDate,
        category: String,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.eq("rankings.dominant_category", category),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.dominantCategory == category }
            }.sortedByDescending { entry -> entry.distribution[category] ?: 0.0 }
    }

    override fun findDistributionTrends(
        entityId: Long,
        months: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("rankings.entity_id", entityId))
            .sort(Sorts.descending("base_date"))
            .limit(months)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSignificantDistributionChanges(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val startEntries =
            collection
                .find(Filters.eq("base_date", startDate.toString()))
                .sort(Sorts.ascending("page"))
                .flatMap { doc ->
                    mongoOperations.converter.read(entityInformation.javaType, doc).rankings
                }.associateBy { it.entityId }

        val endEntries =
            collection
                .find(Filters.eq("base_date", endDate.toString()))
                .sort(Sorts.ascending("page"))
                .flatMap { doc ->
                    mongoOperations.converter.read(entityInformation.javaType, doc).rankings
                }.filter { entry ->
                    val startEntry = startEntries[entry.entityId]
                    if (startEntry != null) {
                        calculateDistributionChange(startEntry.distribution, entry.distribution) > 0.1
                    } else {
                        false
                    }
                }.sortedByDescending { entry ->
                    val startDist = startEntries[entry.entityId]?.distribution
                    if (startDist != null) {
                        calculateDistributionChange(startDist, entry.distribution)
                    } else {
                        0.0
                    }
                }

        return endEntries
    }

    override fun findSimilarDistributions(
        entityId: Long,
        baseDate: BaseDate,
        similarity: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val targetEntry =
            findByEntityId(baseDate.toString(), entityId)
                ?: throw IllegalArgumentException("Base entity not found")

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.ne("rankings.entity_id", entityId),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter.read(entityInformation.javaType, doc).rankings.filter { entry ->
                    calculateCosineSimilarity(targetEntry.distribution, entry.distribution) >= similarity
                }
            }.sortedByDescending { entry ->
                calculateCosineSimilarity(targetEntry.distribution, entry.distribution)
            }
    }

    override fun findUniformDistributions(
        baseDate: BaseDate,
        maxVariance: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.lte("rankings.distribution_metrics.uniformity", maxVariance),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.distributionMetrics.uniformity <= maxVariance }
            }.sortedBy { it.distributionMetrics.uniformity }
    }

    override fun findSkewedDistributions(
        baseDate: BaseDate,
        minSkewness: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.gte("rankings.distribution_metrics.concentration", minSkewness),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { it.distributionMetrics.concentration >= minSkewness }
            }.sortedByDescending { it.distributionMetrics.concentration }
    }

    override fun findDistributionChanges(
        entityId: Long,
        months: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("rankings.entity_id", entityId))
            .sort(Sorts.descending("base_date"))
            .limit(months)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findCategoryDominance(
        baseDate: BaseDate,
        category: String,
        minPercentage: Double,
    ): List<E> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.gte("rankings.distribution.$category", minPercentage),
                ),
            ).sort(Sorts.ascending("page"))
            .flatMap { doc ->
                mongoOperations.converter
                    .read(entityInformation.javaType, doc)
                    .rankings
                    .filter { (it.distribution[category] ?: 0.0) >= minPercentage }
            }.sortedByDescending { it.distribution[category] }
    }

    private fun calculateSimilarity(
        dist1: Map<String, Double>,
        dist2: Map<String, Double>,
    ): Double {
        val allCategories = (dist1.keys + dist2.keys).toSet()
        return 1.0 - allCategories.sumOf { category ->
            val value1 = dist1[category] ?: 0.0
            val value2 = dist2[category] ?: 0.0
            kotlin.math.abs(value1 - value2)
        } / allCategories.size
    }

    private fun calculateDistributionChange(
        dist1: Map<String, Double>,
        dist2: Map<String, Double>,
    ): Double {
        val allCategories = (dist1.keys + dist2.keys).toSet()
        return kotlin.math.sqrt(
            allCategories.sumOf { category ->
                val value1 = dist1[category] ?: 0.0
                val value2 = dist2[category] ?: 0.0
                (value2 - value1) * (value2 - value1)
            },
        )
    }

    private fun calculateCosineSimilarity(
        dist1: Map<String, Double>,
        dist2: Map<String, Double>,
    ): Double {
        val allCategories = (dist1.keys + dist2.keys).toSet()
        val dotProduct =
            allCategories.sumOf { category ->
                (dist1[category] ?: 0.0) * (dist2[category] ?: 0.0)
            }
        val norm1 = kotlin.math.sqrt(dist1.values.sumOf { it * it })
        val norm2 = kotlin.math.sqrt(dist2.values.sumOf { it * it })
        return if (norm1 != 0.0 && norm2 != 0.0) dotProduct / (norm1 * norm2) else 0.0
    }
}
