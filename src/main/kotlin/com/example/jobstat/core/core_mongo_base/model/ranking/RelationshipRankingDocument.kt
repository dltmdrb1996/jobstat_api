package com.example.jobstat.core.core_mongo_base.model.ranking

import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.core.core_model.EntityType
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes

// Relationship Ranking Document
@CompoundIndexes(
    CompoundIndex(
        name = "primary_entity_idx",
        def = "{'primary_entity_type': 1, 'rankings.primary_entity_id': 1, 'base_date': -1}",
    ),
    CompoundIndex(
        name = "related_entity_idx",
        def = "{'related_entity_type': 1, 'rankings.related_rankings.entity_id': 1, 'base_date': -1}",
    ),
)
abstract class RelationshipRankingDocument<T : RelationshipRankingDocument.RelationshipRankingEntry>(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Transient
    override val metrics: RankingMetrics,
    @Transient
    open val primaryEntityType: EntityType,
    @Transient
    open val relatedEntityType: EntityType,
    @Transient
    override val rankings: List<T>,
    page: Int,
) : BaseRankingDocument<T>(id, baseDate, period, metrics, rankings, page) {
    interface RelationshipRankingEntry : RankingEntry {
        val primaryEntityId: Long
        val primaryEntityName: String
        val relatedRankings: List<RelatedEntityRank>
    }

    interface RelatedEntityRank {
        val entityId: Long
        val name: String
        val rank: Int
        val score: Double
    }
}
