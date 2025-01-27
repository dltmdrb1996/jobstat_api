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

@Document(collection = "location_stats_monthly")
class LocationStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: LocationStats,
    @Field("industry_distribution")
    val industryDistribution: List<LocationIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<LocationJobCategory>,
    @Field("company_distribution")
    val companyDistribution: List<LocationCompany>,
    @Field("skill_distribution")
    val skillDistribution: List<LocationSkill>,
    @Field("experience_distribution")
    val experienceDistribution: List<LocationExperience>,
    @Field("education_distribution")
    val educationDistribution: List<LocationEducation>,
    @Field("employment_metrics")
    val employmentMetrics: LocationEmploymentMetrics,
    @Field("living_metrics")
    val livingMetrics: LocationLivingMetrics,
    @Field("remote_work_stats")
    val remoteWorkStats: LocationRemoteWorkStats,
    @Field("rankings")
    override val rankings: Map<RankingType, LocationRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class LocationStats(
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
        @Field("company_count")
        val companyCount: Int,
        @Field("employment_rate")
        val employmentRate: Double,
        @Field("job_seeker_per_opening")
        val jobSeekerPerOpening: Double,
        @Field("avg_commute_time")
        val avgCommuteTime: Double,
        @Field("tech_hub_score")
        val techHubScore: Double,
        @Field("startup_density")
        val startupDensity: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class LocationIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("company_count")
        val companyCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("employment_share")
        val employmentShare: Double,
        @Field("specialization_index")
        val specializationIndex: Double,
    )

    data class LocationJobCategory(
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
        @Field("remote_ratio")
        val remoteRatio: Double,
        @Field("demand_score")
        val demandScore: Double,
        @Field("competition_rate")
        val competitionRate: Double,
    )

    data class LocationCompany(
        @Field("company_id")
        val companyId: Long,
        @Field("name")
        val name: String,
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("employee_count")
        val employeeCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("hiring_velocity")
        val hiringVelocity: Double,
    )

    data class LocationSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("demand_score")
        val demandScore: Double,
        @Field("local_premium")
        val localPremium: Double,
    )

    data class LocationExperience(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("remote_ratio")
        val remoteRatio: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class LocationEducation(
        @Field("level")
        val level: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("demand_score")
        val demandScore: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class LocationEmploymentMetrics(
        @Field("unemployment_rate")
        val unemploymentRate: Double,
        @Field("labor_force_participation")
        val laborForceParticipation: Double,
        @Field("job_growth_rate")
        val jobGrowthRate: Double,
        @Field("median_tenure")
        val medianTenure: Double,
        @Field("workforce_diversity")
        val workforceDiversity: WorkforceDiversity,
    ) {
        data class WorkforceDiversity(
            @Field("gender_ratio")
            val genderRatio: Map<String, Double>,
            @Field("age_distribution")
            val ageDistribution: Map<String, Double>,
        )
    }

    data class LocationLivingMetrics(
        @Field("cost_of_living_index")
        val costOfLivingIndex: Double,
        @Field("housing_affordability")
        val housingAffordability: Double,
        @Field("avg_commute_time")
        val avgCommuteTime: Double,
        @Field("quality_of_life_score")
        val qualityOfLifeScore: Double,
        @Field("salary_to_cost_ratio")
        val salaryToCostRatio: Double,
        @Field("amenities_score")
        val amenitiesScore: Double,
    )

    data class LocationRemoteWorkStats(
        @Field("remote_job_ratio")
        val remoteJobRatio: Double,
        @Field("hybrid_job_ratio")
        val hybridJobRatio: Double,
        @Field("avg_remote_salary")
        val avgRemoteSalary: Long,
        @Field("remote_work_growth")
        val remoteWorkGrowth: Double,
        @Field("top_remote_employers")
        val topRemoteEmployers: List<RemoteEmployer>,
    ) {
        data class RemoteEmployer(
            @Field("company_id")
            val companyId: Long,
            @Field("name")
            val name: String,
            @Field("remote_job_count")
            val remoteJobCount: Int,
        )
    }

    data class LocationRankingInfo(
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
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        name: String = this.name,
        stats: LocationStats = this.stats,
        industryDistribution: List<LocationIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<LocationJobCategory> = this.jobCategoryDistribution,
        companyDistribution: List<LocationCompany> = this.companyDistribution,
        skillDistribution: List<LocationSkill> = this.skillDistribution,
        experienceDistribution: List<LocationExperience> = this.experienceDistribution,
        educationDistribution: List<LocationEducation> = this.educationDistribution,
        employmentMetrics: LocationEmploymentMetrics = this.employmentMetrics,
        livingMetrics: LocationLivingMetrics = this.livingMetrics,
        remoteWorkStats: LocationRemoteWorkStats = this.remoteWorkStats,
        rankings: Map<RankingType, LocationRankingInfo> = this.rankings,
    ) = LocationStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        name = name,
        stats = stats,
        industryDistribution = industryDistribution,
        jobCategoryDistribution = jobCategoryDistribution,
        companyDistribution = companyDistribution,
        skillDistribution = skillDistribution,
        experienceDistribution = experienceDistribution,
        educationDistribution = educationDistribution,
        employmentMetrics = employmentMetrics,
        livingMetrics = livingMetrics,
        remoteWorkStats = remoteWorkStats,
        rankings = rankings,
    )
}
