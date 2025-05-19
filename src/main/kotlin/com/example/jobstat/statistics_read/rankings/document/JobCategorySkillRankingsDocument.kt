package com.example.jobstat.statistics_read.rankings.document

import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.core.core_mongo_base.model.ranking.RankingMetrics
import com.example.jobstat.core.core_mongo_base.model.ranking.RelationshipRankingDocument
import com.example.jobstat.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.example.jobstat.core.core_model.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "job_category_skill_rankings")
class JobCategorySkillRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: JobCategorySkillMetrics,
    @Field("primary_entity_type")
    override val primaryEntityType: EntityType = EntityType.JOB_CATEGORY,
    @Field("related_entity_type")
    override val relatedEntityType: EntityType = EntityType.SKILL,
    @Field("rankings")
    override val rankings: List<JobCategorySkillRankingEntry>,
) : RelationshipRankingDocument<JobCategorySkillRankingsDocument.JobCategorySkillRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        primaryEntityType,
        relatedEntityType,
        rankings,
        page,
    ) {
    data class JobCategorySkillMetrics(
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
        @Field("skill_correlation")
        val skillCorrelation: SkillCorrelation,
    ) : RankingMetrics {
        data class SkillCorrelation(
            @Field("skill_overlap_matrix")
            val skillOverlapMatrix: Map<Long, Map<Long, Double>>,
            @Field("complementary_skills")
            val complementarySkills: List<SkillPair>,
        ) {
            data class SkillPair(
                val skillId1: Long,
                val skillId2: Long,
                val correlationScore: Double,
            )
        }
    }

    data class JobCategorySkillRankingEntry(
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
        @Field("skill_diversity")
        val skillDiversity: Double,
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
            @Field("required_rate")
            val requiredRate: Double,
            @Field("growth_rate")
            val growthRate: Double,
            @Field("importance_score")
            val importanceScore: Double,
        ) : RelatedEntityRank
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(rankings.all { it.relatedRankings.isNotEmpty() }) { "모든 직무 카테고리는 관련 기술을 가지고 있어야 합니다" }
    }
}
