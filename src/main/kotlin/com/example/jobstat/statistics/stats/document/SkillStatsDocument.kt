package com.example.jobstat.statistics.stats.document

import com.example.jobstat.core.base.mongo.CommonDistribution
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.mongo.stats.CommonStats
import com.example.jobstat.core.base.mongo.stats.RankingInfo
import com.example.jobstat.core.base.mongo.stats.RankingScore
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "skill_stats_monthly")
class SkillStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: SkillStats,
    @Field("experience_levels")
    val experienceLevels: List<SkillExperienceLevel>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<CompanySizeDistribution>,
    @Field("industry_distribution")
    val industryDistribution: List<IndustryDistribution>,
    @Field("soft_skill")
    val isSoftSkill: Boolean,
    @Field("emerging_skill")
    val isEmergingSkill: Boolean,
    @Field("related_job_categories")
    val relatedJobCategories: List<RelatedJobCategory>,
    @Field("rankings")
    override val rankings: Map<RankingType, SkillRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, mapOf()) {
    override fun validate() {
//        require(experienceLevels.isNotEmpty()) { "경력 수준이 비어있으면 안됩니다" }
//        require(companySizeDistribution.isNotEmpty()) { "회사 규모 분포가 비어있으면 안됩니다" }
//        require(industryDistribution.isNotEmpty()) { "산업 분포가 비어있으면 안됩니다" }
//        require(relatedJobCategories.isNotEmpty()) { "관련 직무 카테고리가 비어있으면 안됩니다" }
    }

    data class SkillRankingInfo(
        @Field("current_rank")
        override val currentRank: Int,
        @Field("previous_rank")
        override val previousRank: Int?,
        @Field("rank_change")
        override val rankChange: Int?,
        @Field("percentile")
        override val percentile: Double?,
        @Field("ranking_score")
        override val rankingScore: RankingScore,
    ) : RankingInfo

    data class SkillStats(
        @Field("posting_count")
        override val postingCount: Int,
        @Field("active_posting_count")
        override val activePostingCount: Int,
        @Field("avg_salary")
        override val avgSalary: Long,
        @Field("growth_rate")
        override val growthRate: Double,
        @Field("year_over_year_growth")
        override val yearOverYearGrowth: Double?,
        @Field("month_over_month_change")
        override val monthOverMonthChange: Double?,
        @Field("demand_trend")
        override val demandTrend: String,
        @Field("demand_score")
        val demandScore: Double,
        @Field("company_count")
        val companyCount: Int,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class SkillExperienceLevel(
        @Field("range")
        val range: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        override val avgSalary: Long,
    ) : CommonDistribution(postingCount, 0.0, avgSalary)

    data class CompanySizeDistribution(
        @Field("company_size")
        val companySize: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
    ) : CommonDistribution(count, 0.0, avgSalary)

    data class IndustryDistribution(
        @Field("industry_id")
        val industryId: Long,
        @Field("industry_name")
        val industryName: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
    ) : CommonDistribution(count, 0.0, avgSalary)

    data class RelatedJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("importance_score")
        val importanceScore: Double,
        @Field("growth_rate")
        val growthRate: Double,
    )

    fun copy(
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        name: String = this.name,
        stats: SkillStats = this.stats,
        experienceLevels: List<SkillExperienceLevel> = this.experienceLevels,
        companySizeDistribution: List<CompanySizeDistribution> = this.companySizeDistribution,
        industryDistribution: List<IndustryDistribution> = this.industryDistribution,
        isSoftSkill: Boolean = this.isSoftSkill,
        isEmergingSkill: Boolean = this.isEmergingSkill,
        relatedJobCategories: List<RelatedJobCategory> = this.relatedJobCategories,
        rankings: Map<RankingType, SkillRankingInfo> = this.rankings,
    ): SkillStatsDocument =
        SkillStatsDocument(
            id = this.id,
            entityId = entityId,
            baseDate = baseDate,
            period = period,
            name = name,
            stats = stats,
            experienceLevels = experienceLevels,
            companySizeDistribution = companySizeDistribution,
            industryDistribution = industryDistribution,
            isSoftSkill = isSoftSkill,
            isEmergingSkill = isEmergingSkill,
            relatedJobCategories = relatedJobCategories,
            rankings = rankings,
        )
}
