package com.wildrew.jobstat.statistics_read.stats.document

import com.wildrew.jobstat.statistics_read.core.core_model.CompanySize
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.CommonDistribution
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.CommonStats
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingInfo
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingScore
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "certification_stats_monthly")
class CertificationStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: CertificationStats,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<CertificationJobCategory>,
    @Field("industry_distribution")
    val industryDistribution: List<CertificationIndustry>,
    @Field("skill_correlations")
    val skillCorrelations: List<CertificationSkill>,
    @Field("experience_distribution")
    val experienceDistribution: List<CertificationExperience>,
    @Field("company_distribution")
    val companyDistribution: List<CertificationCompany>,
    @Field("location_distribution")
    val locationDistribution: List<CertificationLocation>,
    @Field("exam_metrics")
    val examMetrics: CertificationExamMetrics,
    @Field("career_impact")
    val careerImpact: CertificationCareerImpact,
    @Field("investment_metrics")
    val investmentMetrics: CertificationInvestmentMetrics,
    @Field("rankings")
    override val rankings: Map<RankingType, CertificationRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class CertificationStats(
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
        @Field("required_count")
        val requiredCount: Int,
        @Field("preferred_count")
        val preferredCount: Int,
        @Field("certificate_holder_count")
        val certificateHolderCount: Int,
        @Field("salary_premium")
        val salaryPremium: Double,
        @Field("market_value_score")
        val marketValueScore: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class CertificationJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("preference_rate")
        val preferenceRate: Double,
        @Field("salary_premium")
        val salaryPremium: Double,
        @Field("career_advancement_rate")
        val careerAdvancementRate: Double,
    )

    data class CertificationIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("industry_importance")
        val industryImportance: Double,
    )

    data class CertificationSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("correlation_score")
        val correlationScore: Double,
        @Field("co_occurrence_count")
        val coOccurrenceCount: Int,
        @Field("skill_synergy")
        val skillSynergy: Double,
        @Field("combined_salary_premium")
        val combinedSalaryPremium: Double,
    )

    data class CertificationExperience(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("value_add_score")
        val valueAddScore: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CertificationCompany(
        @Field("company_id")
        val companyId: Long,
        @Field("name")
        val name: String,
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("value_recognition")
        val valueRecognition: Double,
    )

    data class CertificationLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("demand_index")
        val demandIndex: Double,
        @Field("local_importance")
        val localImportance: Double,
    )

    data class CertificationExamMetrics(
        @Field("pass_rate")
        val passRate: Double,
        @Field("avg_preparation_time")
        val avgPreparationTime: Int,
        @Field("exam_frequency")
        val examFrequency: String,
        @Field("avg_attempts")
        val avgAttempts: Double,
        @Field("difficulty_rating")
        val difficultyRating: Double,
        @Field("preparation_resources")
        val preparationResources: PreparationResources,
    ) {
        data class PreparationResources(
            @Field("recommended_study_time")
            val recommendedStudyTime: Int,
            @Field("material_costs")
            val materialCosts: Long,
            @Field("training_availability")
            val trainingAvailability: Double,
        )
    }

    data class CertificationCareerImpact(
        @Field("salary_increase_rate")
        val salaryIncreaseRate: Double,
        @Field("promotion_rate")
        val promotionRate: Double,
        @Field("career_advancement_time")
        val careerAdvancementTime: Double,
        @Field("job_opportunity_increase")
        val jobOpportunityIncrease: Double,
        @Field("industry_mobility")
        val industryMobility: Double,
        @Field("skill_development_opportunities")
        val skillDevelopmentOpportunities: List<SkillOpportunity>,
    ) {
        data class SkillOpportunity(
            @Field("skill_id")
            val skillId: Long,
            @Field("name")
            val name: String,
            @Field("relevance_score")
            val relevanceScore: Double,
        )
    }

    data class CertificationInvestmentMetrics(
        @Field("total_cost")
        val totalCost: Long,
        @Field("roi_timeframe")
        val roiTimeframe: Double,
        @Field("salary_roi")
        val salaryRoi: Double,
        @Field("career_value_score")
        val careerValueScore: Double,
        @Field("market_demand_score")
        val marketDemandScore: Double,
        @Field("investment_rating")
        val investmentRating: Double,
    )

    data class CertificationRankingInfo(
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
        @Field("value_change")
        override val valueChange: Double?,
    ) : RankingInfo

    override fun validate() {
        TODO("Not yet implemented")
    }

    fun copy(
        id: String? = this.id,
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        name: String = this.name,
        stats: CertificationStats = this.stats,
        jobCategoryDistribution: List<CertificationJobCategory> = this.jobCategoryDistribution,
        industryDistribution: List<CertificationIndustry> = this.industryDistribution,
        skillCorrelations: List<CertificationSkill> = this.skillCorrelations,
        experienceDistribution: List<CertificationExperience> = this.experienceDistribution,
        companyDistribution: List<CertificationCompany> = this.companyDistribution,
        locationDistribution: List<CertificationLocation> = this.locationDistribution,
        examMetrics: CertificationExamMetrics = this.examMetrics,
        careerImpact: CertificationCareerImpact = this.careerImpact,
        investmentMetrics: CertificationInvestmentMetrics = this.investmentMetrics,
        rankings: Map<RankingType, CertificationRankingInfo> = this.rankings,
    ) = CertificationStatsDocument(
        id,
        entityId,
        baseDate,
        period,
        name,
        stats,
        jobCategoryDistribution,
        industryDistribution,
        skillCorrelations,
        experienceDistribution,
        companyDistribution,
        locationDistribution,
        examMetrics,
        careerImpact,
        investmentMetrics,
        rankings,
    )
}
