package com.wildrew.app.statistics_read.rankings.document

import com.wildrew.app.statistics_read.core.core_model.EntityType
import com.wildrew.app.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.RelationshipRankingDocument
import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "industry_skill_rankings")
class IndustrySkillRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: IndustrySkillMetrics,
    @Field("primary_entity_type")
    override val primaryEntityType: EntityType = EntityType.INDUSTRY,
    @Field("related_entity_type")
    override val relatedEntityType: EntityType = EntityType.SKILL,
    @Field("rankings")
    override val rankings: List<IndustrySkillRankingEntry>,
) : RelationshipRankingDocument<IndustrySkillRankingsDocument.IndustrySkillRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        primaryEntityType,
        relatedEntityType,
        rankings,
        page,
    ) {
    data class IndustrySkillMetrics(
        @Field("total_count")
        override val totalCount: Int,
        @Field("ranked_count")
        override val rankedCount: Int,
        @Field("new_entries")
        override val newEntries: Int,
        @Field("dropped_entries")
        override val droppedEntries: Int,
        @Field("volatility_metrics")
        override val volatilityMetrics: VolatilityMetrics,
        @Field("industry_skill_correlation")
        val industrySkillCorrelation: IndustrySkillCorrelation,
    ) : RankingMetrics {
        data class IndustrySkillCorrelation(
            @Field("cross_industry_skills")
            val crossIndustrySkills: Map<Long, Map<Long, Double>>,
            @Field("skill_transition_patterns")
            val skillTransitionPatterns: List<SkillTransition>,
        ) {
            data class SkillTransition(
                val fromIndustryId: Long,
                val toIndustryId: Long,
                val commonSkillsCount: Int,
                val transitionScore: Double,
            )
        }
    }

    data class IndustrySkillRankingEntry(
        @Field("entity_id")
        override val entityId: Long,
        @Field("name")
        override val name: String,
        @Field("rank")
        override val rank: Int,
        @Field("previous_rank")
        override val previousRank: Int?,
        @Field("rank_change")
        override val rankChange: Int?,
        @Field("primary_entity_id")
        override val primaryEntityId: Long,
        @Field("primary_entity_name")
        override val primaryEntityName: String,
        @Field("related_rankings")
        override val relatedRankings: List<SkillRank>,
        @Field("total_postings")
        val totalPostings: Int,
        @Field("industry_penetration")
        val industryPenetration: Double,
    ) : RelationshipRankingEntry {
        data class SkillRank(
            @Field("entity_id")
            override val entityId: Long,
            @Field("name")
            override val name: String,
            @Field("rank")
            override val rank: Int,
            @Field("score")
            override val score: Double,
            @Field("demand_level")
            val demandLevel: Double,
            @Field("growth_rate")
            val growthRate: Double,
            @Field("industry_specificity")
            val industrySpecificity: Double,
        ) : RelatedEntityRank
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(rankings.all { it.relatedRankings.isNotEmpty() }) { "모든 산업은 관련 기술을 가지고 있어야 합니다" }
    }
}
