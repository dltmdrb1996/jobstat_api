package com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking

import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes

@CompoundIndexes(
    CompoundIndex(
        name = "group_entity_idx", // 그룹 엔티티 인덱스
        def = "{'group_entity_type': 1, 'rankings.entity_id': 1, 'base_date': -1}",
    ),
    CompoundIndex(
        name = "distribution_pattern_idx", // 분포 패턴 인덱스
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
    page: Int,
) : BaseRankingDocument<T>(id, baseDate, period, metrics, rankings, page) {
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
