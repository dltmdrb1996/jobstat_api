package com.wildrew.jobstat.statistics_read.stats.document

import com.wildrew.jobstat.statistics_read.core.core_model.CompanySize
import com.wildrew.jobstat.statistics_read.core.core_model.EducationLevel
import com.wildrew.jobstat.statistics_read.core.core_model.ExperienceLevel
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.CommonStats
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingInfo
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingScore
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_size_stats_monthly")
class CompanySizeStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("size")
    val size: CompanySize,
    @Field("stats")
    override val stats: CompanySizeStats,
    @Field("industry_distribution")
    val industryDistribution: List<CompanySizeIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<CompanySizeJobCategory>,
    @Field("location_distribution")
    val locationDistribution: List<CompanySizeLocation>,
    @Field("skill_distribution")
    val skillDistribution: List<CompanySizeSkill>,
    @Field("benefit_distribution")
    val benefitDistribution: List<CompanySizeBenefit>,
    @Field("education_level_distribution")
    val educationLevelDistribution: List<CompanySizeEducationLevel>,
    @Field("work_environment_metrics")
    val workEnvironmentMetrics: WorkEnvironmentMetrics,
    @Field("hiring_profile")
    val hiringProfile: HiringProfile,
    @Field("compensation_package")
    val compensationPackage: CompensationPackage,
    @Field("rankings")
    override val rankings: Map<RankingType, CompanySizeRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class CompanySizeStats(
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
        @Field("avg_employee_tenure")
        val avgEmployeeTenure: Double,
        @Field("hiring_volume_growth")
        val hiringVolumeGrowth: Double,
        @Field("benefit_provision_rate")
        val benefitProvisionRate: Double,
        @Field("avg_years_of_experience_required")
        val avgYearsOfExperienceRequired: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class CompanySizeIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("dominance_score")
        val dominanceScore: Double,
    )

    data class CompanySizeJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("hiring_preference")
        val hiringPreference: Double,
    )

    data class CompanySizeLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("market_share")
        val marketShare: Double,
    )

    data class CompanySizeSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("requirement_frequency")
        val requirementFrequency: Double,
        @Field("salary_impact")
        val salaryImpact: Double,
    )

    data class CompanySizeBenefit(
        @Field("benefit_id")
        val benefitId: Long,
        @Field("name")
        val name: String,
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("impact_on_satisfaction")
        val impactOnSatisfaction: Double,
    )

    data class CompanySizeEducationLevel(
        @Field("level")
        val level: EducationLevel,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("preference_index")
        val preferenceIndex: Double,
    )

    data class WorkEnvironmentMetrics(
        @Field("work_life_balance_score")
        val workLifeBalanceScore: Double,
        @Field("career_growth_opportunity_score")
        val careerGrowthOpportunityScore: Double,
        @Field("remote_work_adoption_rate")
        val remoteWorkAdoptionRate: Double,
    )

    data class HiringProfile(
        @Field("experience_level_distribution")
        val experienceLevelDistribution: Map<ExperienceLevel, Double>,
        @Field("preferred_education_levels")
        val preferredEducationLevels: List<String>,
        @Field("hiring_speed")
        val hiringSpeed: Double,
    )

    data class CompensationPackage(
        @Field("salary_competitiveness_index")
        val salaryCompetitivenessIndex: Double,
        @Field("bonus_and_incentive_prevalence")
        val bonusAndIncentivePrevalence: Double,
        @Field("equity_granting_rate")
        val equityGrantingRate: Double,
    )

    data class CompanySizeRankingInfo(
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
    }

    fun copy(
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        size: CompanySize = this.size,
        stats: CompanySizeStats = this.stats,
        industryDistribution: List<CompanySizeIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<CompanySizeJobCategory> = this.jobCategoryDistribution,
        locationDistribution: List<CompanySizeLocation> = this.locationDistribution,
        skillDistribution: List<CompanySizeSkill> = this.skillDistribution,
        benefitDistribution: List<CompanySizeBenefit> = this.benefitDistribution,
        educationLevelDistribution: List<CompanySizeEducationLevel> = this.educationLevelDistribution,
        workEnvironmentMetrics: WorkEnvironmentMetrics = this.workEnvironmentMetrics,
        hiringProfile: HiringProfile = this.hiringProfile,
        compensationPackage: CompensationPackage = this.compensationPackage,
        rankings: Map<RankingType, CompanySizeRankingInfo> = this.rankings,
    ) = CompanySizeStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        size = size,
        stats = stats,
        industryDistribution = industryDistribution,
        jobCategoryDistribution = jobCategoryDistribution,
        locationDistribution = locationDistribution,
        skillDistribution = skillDistribution,
        benefitDistribution = benefitDistribution,
        educationLevelDistribution = educationLevelDistribution,
        workEnvironmentMetrics = workEnvironmentMetrics,
        hiringProfile = hiringProfile,
        compensationPackage = compensationPackage,
        rankings = rankings,
    )
}
