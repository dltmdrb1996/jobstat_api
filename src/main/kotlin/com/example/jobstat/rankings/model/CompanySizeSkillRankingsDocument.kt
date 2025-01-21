package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.RelationshipRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import com.example.jobstat.core.state.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_size_skill_rankings")
class CompanySizeSkillRankingsDocument(
    id: String? = null,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanySizeSkillMetrics,
    @Field("primary_entity_type")
    override val primaryEntityType: EntityType = EntityType.COMPANY_SIZE,
    @Field("related_entity_type")
    override val relatedEntityType: EntityType = EntityType.SKILL,
    @Field("rankings")
    override val rankings: List<CompanySizeSkillRankingEntry>,
) : RelationshipRankingDocument<CompanySizeSkillRankingsDocument.CompanySizeSkillRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        primaryEntityType,
        relatedEntityType,
        rankings,
    ) {
    data class CompanySizeSkillMetrics(
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
        @Field("size_skill_correlation")
        val sizeSkillCorrelation: SizeSkillCorrelation,
    ) : RankingMetrics {
        data class SizeSkillCorrelation(
            @Field("adoption_patterns")
            val adoptionPatterns: Map<String, Map<Long, Double>>,
            @Field("skill_complexity")
            val skillComplexity: List<SizeComplexity>,
        ) {
            data class SizeComplexity(
                val companySize: String,
                val averageSkillCount: Double,
                val complexityScore: Double,
            )
        }
    }

    data class CompanySizeSkillRankingEntry(
        @Field("document_id")
        override val documentId: String,
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
        @Field("skill_adoption_rate")
        val skillAdoptionRate: Double,
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
            @Field("requirement_level")
            val requirementLevel: Double,
            @Field("growth_rate")
            val growthRate: Double,
            @Field("size_relevance")
            val sizeRelevance: Double,
        ) : RelatedEntityRank
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.relatedRankings.isNotEmpty() }) { "All company sizes must have related skills" }
    }
}
