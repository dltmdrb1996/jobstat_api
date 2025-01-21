package com.example.jobstat.core.base.mongo.ranking

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.state.EntityType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.annotation.Transient

@CompoundIndexes(
    CompoundIndex(
        name = "group_entity_idx",
        def = "{'group_entity_type': 1, 'rankings.entity_id': 1, 'base_date': -1}",
    ),
    CompoundIndex(
        name = "distribution_pattern_idx",
        def = "{'base_date': -1, 'rankings.dominant_category': 1, 'rankings.distribution_metrics.concentration': -1}",
    ),
)
abstract class DistributionRankingDocument<T : DistributionRankingDocument.DistributionRankingEntry>(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Transient
    override val metrics: RankingMetrics,
    @Transient
    open val groupEntityType: EntityType,
    @Transient
    open val targetEntityType: EntityType,
    @Transient
    override val rankings: List<T>,
) : BaseRankingDocument<T>(id, baseDate, period, metrics, rankings) {
    interface DistributionRankingEntry : RankingEntry {
        val distribution: Map<String, Double>
        val dominantCategory: String
        val distributionMetrics: DistributionMetrics
    }

    data class DistributionMetrics(
        val entropy: Double,
        val concentration: Double,
        val uniformity: Double,
    )
}
