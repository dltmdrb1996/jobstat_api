package com.example.jobstat.stats.model

import com.example.jobstat.core.base.mongo.CommonDistribution
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingType
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.mongo.stats.CommonStats
import com.example.jobstat.core.base.mongo.stats.RankingInfo
import com.example.jobstat.core.base.mongo.stats.RankingScore
import com.example.jobstat.core.state.CompanySize
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_stats_monthly")
class CompanyStatsDocument(
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
    override val stats: CompanyStats,
    @Field("size")
    val size: CompanySize,
    @Field("industry_id")
    val industryId: Long,
    @Field("job_categories")
    val jobCategories: List<CompanyJobCategory>,
    @Field("skills")
    val skills: List<CompanySkill>,
    @Field("benefits")
    val benefits: List<CompanyBenefit>,
    @Field("experience_distribution")
    val experienceDistribution: List<CompanyExperienceDistribution>,
    @Field("education_distribution")
    val educationDistribution: List<CompanyEducationDistribution>,
    @Field("location_distribution")
    val locationDistribution: List<CompanyLocationDistribution>,
    @Field("hiring_trends")
    val hiringTrends: CompanyHiringTrends,
    @Field("remote_work_ratio")
    val remoteWorkRatio: Double,
    @Field("contract_type_distribution")
    val contractTypeDistribution: List<ContractTypeDistribution>,
    @Field("employee_satisfaction")
    val employeeSatisfaction: CompanyEmployeeSatisfaction,
    @Field("rankings")
    override val rankings: Map<RankingType,  CompanyRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class CompanyStats(
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
        @Field("employee_count")
        val employeeCount: Int,
        @Field("application_count")
        val applicationCount: Int,
        @Field("avg_time_to_hire")
        val avgTimeToHire: Double,
        @Field("employee_retention_rate")
        val employeeRetentionRate: Double,
        @Field("revenue_per_employee")
        val revenuePerEmployee: Long,
        @Field("market_share")
        val marketShare: Double,
        @Field("entry_level_ratio")
        val entryLevelRatio: Double,
        @Field("remote_work_ratio")
        val remoteWorkRatio: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class CompanyJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("skill_requirements")
        val skillRequirements: List<JobCategorySkillRequirement>,
        @Field("hiring_difficulty_score")
        val hiringDifficultyScore: Double,
        @Field("turnover_rate")
        val turnoverRate: Double,
    ) {
        data class JobCategorySkillRequirement(
            @Field("skill_id")
            val skillId: Long,
            @Field("name")
            val name: String,
            @Field("required_count")
            val requiredCount: Int,
            @Field("importance_score")
            val importanceScore: Double,
        )
    }

    data class CompanySkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("required_ratio")
        val requiredRatio: Double,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("experience_requirement_avg")
        val experienceRequirementAvg: Double,
        @Field("market_competitiveness")
        val marketCompetitiveness: Double,
    )

    data class CompanyBenefit(
        @Field("benefit_id")
        val benefitId: Long,
        @Field("name")
        val name: String,
        @Field("provision_ratio")
        val provisionRatio: Double,
        @Field("satisfaction_score")
        val satisfactionScore: Double,
        @Field("market_comparison")
        val marketComparison: Double,
        @Field("cost_per_employee")
        val costPerEmployee: Long?,
    )

    data class CompanyExperienceDistribution(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("turnover_rate")
        val turnoverRate: Double,
        @Field("promotion_rate")
        val promotionRate: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CompanyEducationDistribution(
        @Field("level")
        val level: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("required_ratio")
        val requiredRatio: Double,
        @Field("preferred_ratio")
        val preferredRatio: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CompanyLocationDistribution(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("remote_work_ratio")
        val remoteWorkRatio: Double,
        @Field("office_cost")
        val officeCost: Long?,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CompanyHiringTrends(
        @Field("monthly_hiring_velocity")
        val monthlyHiringVelocity: Double,
        @Field("seasonal_patterns")
        val seasonalPatterns: Map<String, Double>,
        @Field("growth_positions")
        val growthPositions: List<GrowthPosition>,
        @Field("hiring_forecast")
        val hiringForecast: HiringForecast,
    ) {
        data class GrowthPosition(
            @Field("job_category_id")
            val jobCategoryId: Long,
            @Field("name")
            val name: String,
            @Field("growth_rate")
            val growthRate: Double,
            @Field("hiring_plan")
            val hiringPlan: Int,
        )

        data class HiringForecast(
            @Field("next_quarter_estimate")
            val nextQuarterEstimate: Int,
            @Field("year_end_estimate")
            val yearEndEstimate: Int,
            @Field("growth_areas")
            val growthAreas: List<String>,
        )
    }

    data class ContractTypeDistribution(
        @Field("type")
        val type: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CompanyEmployeeSatisfaction(
        @Field("overall_score")
        val overallScore: Double,
        @Field("work_life_balance")
        val workLifeBalance: Double,
        @Field("compensation_benefits")
        val compensationBenefits: Double,
        @Field("career_growth")
        val careerGrowth: Double,
        @Field("management")
        val management: Double,
        @Field("workplace_culture")
        val workplaceCulture: Double,
    )

    data class CompanyRankingInfo(
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

    override fun validate() {
        TODO("Not yet implemented")
    }

    fun copy(
        id: String? = this.id,
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        name: String = this.name,
        stats: CompanyStats = this.stats,
        size: CompanySize = this.size,
        industryId: Long = this.industryId,
        jobCategories: List<CompanyJobCategory> = this.jobCategories,
        skills: List<CompanySkill> = this.skills,
        benefits: List<CompanyBenefit> = this.benefits,
        experienceDistribution: List<CompanyExperienceDistribution> = this.experienceDistribution,
        educationDistribution: List<CompanyEducationDistribution> = this.educationDistribution,
        locationDistribution: List<CompanyLocationDistribution> = this.locationDistribution,
        hiringTrends: CompanyHiringTrends = this.hiringTrends,
        remoteWorkRatio: Double = this.remoteWorkRatio,
        contractTypeDistribution: List<ContractTypeDistribution> = this.contractTypeDistribution,
        employeeSatisfaction: CompanyEmployeeSatisfaction = this.employeeSatisfaction,
        rankings: Map<RankingType, CompanyRankingInfo> = this.rankings,
    ) = CompanyStatsDocument(
        id,
        entityId,
        baseDate,
        period,
        name,
        stats,
        size,
        industryId,
        jobCategories,
        skills,
        benefits,
        experienceDistribution,
        educationDistribution,
        locationDistribution,
        hiringTrends,
        remoteWorkRatio,
        contractTypeDistribution,
        employeeSatisfaction,
        rankings,
    )
}
