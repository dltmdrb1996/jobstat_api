package com.wildrew.app.utils.base

import com.wildrew.jobstat.statistics_read.core.core_model.BaseDate
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.DistributionRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.DistributionRankingRepository
import kotlin.math.sqrt

abstract class AbstractFakeDistributionRankingRepository<
    T : DistributionRankingDocument<E>,
    E : DistributionRankingDocument.DistributionRankingEntry,
> :
    AbstractFakeRankingRepository<T, E>(),
    DistributionRankingRepository<T, E, String> {
    override fun findByDistributionPattern(
        baseDate: BaseDate,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { entry ->
                calculateSimilarity(entry.distribution, pattern) >= threshold
            }.sortedByDescending { entry -> calculateSimilarity(entry.distribution, pattern) }

    override fun findByDominantCategory(
        baseDate: BaseDate,
        category: String,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { it.dominantCategory == category }
            .sortedByDescending { it.distribution[category] ?: 0.0 }

    override fun findDistributionTrends(
        entityId: Long,
        months: Int,
    ): List<T> =
        documents.values
            .filter { doc -> doc.rankings.any { it.entityId == entityId } }
            .sortedByDescending { it.baseDate }
            .take(months)

    override fun findSignificantDistributionChanges(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<E> {
        val startEntries =
            documents.values
                .filter { it.baseDate == startDate.toString() }
                .flatMap { it.rankings }
                .associateBy { it.entityId }

        return documents.values
            .filter { it.baseDate == endDate.toString() }
            .flatMap { it.rankings }
            .filter { entry ->
                val startEntry = startEntries[entry.entityId]
                startEntry != null &&
                    calculateDistributionChange(
                        startEntry.distribution,
                        entry.distribution,
                    ) > 0.1
            }.sortedByDescending { entry ->
                startEntries[entry.entityId]?.let { startEntry ->
                    calculateDistributionChange(startEntry.distribution, entry.distribution)
                } ?: 0.0
            }
    }

    override fun findSimilarDistributions(
        entityId: Long,
        baseDate: BaseDate,
        similarity: Double,
    ): List<E> {
        val targetEntry =
            findByEntityId(baseDate.toString(), entityId)
                ?: throw IllegalArgumentException("Base entity not found")

        return documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { entry ->
                entry.entityId != entityId &&
                    calculateCosineSimilarity(targetEntry.distribution, entry.distribution) >= similarity
            }.sortedByDescending { entry ->
                calculateCosineSimilarity(targetEntry.distribution, entry.distribution)
            }
    }

    override fun findUniformDistributions(
        baseDate: BaseDate,
        maxVariance: Double,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { it.distributionMetrics.uniformity <= maxVariance }
            .sortedBy { it.distributionMetrics.uniformity }

    override fun findSkewedDistributions(
        baseDate: BaseDate,
        minSkewness: Double,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { it.distributionMetrics.concentration >= minSkewness }
            .sortedByDescending { it.distributionMetrics.concentration }

    override fun findDistributionChanges(
        entityId: Long,
        months: Int,
    ): List<T> =
        documents.values
            .filter { doc -> doc.rankings.any { it.entityId == entityId } }
            .sortedByDescending { it.baseDate }
            .take(months)

    override fun findCategoryDominance(
        baseDate: BaseDate,
        category: String,
        minPercentage: Double,
    ): List<E> =
        documents.values
            .filter { it.baseDate == baseDate.toString() }
            .flatMap { it.rankings }
            .filter { (it.distribution[category] ?: 0.0) >= minPercentage }
            .sortedByDescending { it.distribution[category] }

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
        return sqrt(
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
        val norm1 = sqrt(dist1.values.sumOf { it * it })
        val norm2 = sqrt(dist2.values.sumOf { it * it })
        return if (norm1 != 0.0 && norm2 != 0.0) dotProduct / (norm1 * norm2) else 0.0
    }
}
