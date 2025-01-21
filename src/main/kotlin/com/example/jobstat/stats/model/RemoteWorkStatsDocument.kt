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

@Document(collection = "remote_work_stats_monthly")
class RemoteWorkStatsDocument(
    id: String? = null,
    entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("type")
    val type: String, // FULL_REMOTE, HYBRID, NONE
    @Field("stats")
    override val stats: RemoteWorkStats,
    @Field("industry_distribution")
    val industryDistribution: List<RemoteWorkIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<RemoteWorkJobCategory>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<RemoteWorkCompanySize>,
    @Field("location_distribution")
    val locationDistribution: List<RemoteWorkLocation>,
    @Field("skill_distribution")
    val skillDistribution: List<RemoteWorkSkill>,
    @Field("experience_distribution")
    val experienceDistribution: List<RemoteWorkExperience>,
    @Field("productivity_metrics")
    val productivityMetrics: RemoteWorkProductivity,
    @Field("collaboration_metrics")
    val collaborationMetrics: RemoteWorkCollaboration,
    @Field("infrastructure_metrics")
    val infrastructureMetrics: RemoteWorkInfrastructure,
    @Field("satisfaction_metrics")
    val satisfactionMetrics: RemoteWorkSatisfaction,
    @Field("rankings")
    override val rankings: Map<RankingType, RemoteWorkRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class RemoteWorkStats(
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
        @Field("adoption_rate")
        val adoptionRate: Double,
        @Field("retention_rate")
        val retentionRate: Double,
        @Field("cost_savings")
        val costSavings: Double,
        @Field("productivity_index")
        val productivityIndex: Double,
        @Field("application_rate")
        val applicationRate: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class RemoteWorkIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("adoption_rate")
        val adoptionRate: Double,
        @Field("success_rate")
        val successRate: Double,
        @Field("productivity_score")
        val productivityScore: Double,
        @Field("cost_effectiveness")
        val costEffectiveness: Double,
    )

    data class RemoteWorkJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("suitability_score")
        val suitabilityScore: Double,
        @Field("collaboration_index")
        val collaborationIndex: Double,
        @Field("effectiveness_score")
        val effectivenessScore: Double,
    )

    data class RemoteWorkCompanySize(
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("adoption_rate")
        val adoptionRate: Double,
        @Field("infrastructure_score")
        val infrastructureScore: Double,
        @Field("success_metrics")
        val successMetrics: SuccessMetrics,
    ) {
        data class SuccessMetrics(
            @Field("productivity")
            val productivity: Double,
            @Field("employee_satisfaction")
            val employeeSatisfaction: Double,
            @Field("cost_efficiency")
            val costEfficiency: Double,
        )
    }

    data class RemoteWorkLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("adoption_rate")
        val adoptionRate: Double,
        @Field("infrastructure_quality")
        val infrastructureQuality: Double,
        @Field("cost_benefit_ratio")
        val costBenefitRatio: Double,
    )

    data class RemoteWorkSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("importance_score")
        val importanceScore: Double,
        @Field("required_proficiency")
        val requiredProficiency: Double,
        @Field("remote_effectiveness")
        val remoteEffectiveness: Double,
    )

    data class RemoteWorkExperience(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("productivity_score")
        val productivityScore: Double,
        @Field("adaptation_speed")
        val adaptationSpeed: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class RemoteWorkProductivity(
        @Field("overall_productivity")
        val overallProductivity: Double,
        @Field("task_completion_rate")
        val taskCompletionRate: Double,
        @Field("work_quality_score")
        val workQualityScore: Double,
        @Field("time_management")
        val timeManagement: TimeManagement,
        @Field("performance_metrics")
        val performanceMetrics: PerformanceMetrics,
    ) {
        data class TimeManagement(
            @Field("work_hours_efficiency")
            val workHoursEfficiency: Double,
            @Field("meeting_efficiency")
            val meetingEfficiency: Double,
            @Field("focus_time_ratio")
            val focusTimeRatio: Double,
        )

        data class PerformanceMetrics(
            @Field("goal_achievement")
            val goalAchievement: Double,
            @Field("project_delivery")
            val projectDelivery: Double,
            @Field("quality_standards")
            val qualityStandards: Double,
        )
    }

    data class RemoteWorkCollaboration(
        @Field("team_collaboration_score")
        val teamCollaborationScore: Double,
        @Field("communication_effectiveness")
        val communicationEffectiveness: Double,
        @Field("tools_utilization")
        val toolsUtilization: ToolsUtilization,
        @Field("meeting_metrics")
        val meetingMetrics: MeetingMetrics,
    ) {
        data class ToolsUtilization(
            @Field("platform_adoption")
            val platformAdoption: Double,
            @Field("tool_effectiveness")
            val toolEffectiveness: Double,
            @Field("usage_frequency")
            val usageFrequency: Map<String, Double>,
        )

        data class MeetingMetrics(
            @Field("frequency")
            val frequency: Double,
            @Field("effectiveness")
            val effectiveness: Double,
            @Field("participation_rate")
            val participationRate: Double,
        )
    }

    data class RemoteWorkInfrastructure(
        @Field("technology_readiness")
        val technologyReadiness: Double,
        @Field("support_quality")
        val supportQuality: Double,
        @Field("security_measures")
        val securityMeasures: SecurityMeasures,
        @Field("equipment_provision")
        val equipmentProvision: EquipmentProvision,
    ) {
        data class SecurityMeasures(
            @Field("security_score")
            val securityScore: Double,
            @Field("compliance_rate")
            val complianceRate: Double,
            @Field("incident_rate")
            val incidentRate: Double,
        )

        data class EquipmentProvision(
            @Field("provision_rate")
            val provisionRate: Double,
            @Field("equipment_quality")
            val equipmentQuality: Double,
            @Field("support_satisfaction")
            val supportSatisfaction: Double,
        )
    }

    data class RemoteWorkSatisfaction(
        @Field("overall_satisfaction")
        val overallSatisfaction: Double,
        @Field("work_life_balance")
        val workLifeBalance: Double,
        @Field("engagement_metrics")
        val engagementMetrics: EngagementMetrics,
        @Field("wellness_metrics")
        val wellnessMetrics: WellnessMetrics,
    ) {
        data class EngagementMetrics(
            @Field("team_engagement")
            val teamEngagement: Double,
            @Field("career_development")
            val careerDevelopment: Double,
            @Field("recognition_score")
            val recognitionScore: Double,
        )

        data class WellnessMetrics(
            @Field("stress_level")
            val stressLevel: Double,
            @Field("work_environment")
            val workEnvironment: Double,
            @Field("health_initiatives")
            val healthInitiatives: Double,
        )
    }

    data class RemoteWorkRankingInfo(
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
        type: String = this.type,
        stats: RemoteWorkStats = this.stats,
        industryDistribution: List<RemoteWorkIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<RemoteWorkJobCategory> = this.jobCategoryDistribution,
        companySizeDistribution: List<RemoteWorkCompanySize> = this.companySizeDistribution,
        locationDistribution: List<RemoteWorkLocation> = this.locationDistribution,
        skillDistribution: List<RemoteWorkSkill> = this.skillDistribution,
        experienceDistribution: List<RemoteWorkExperience> = this.experienceDistribution,
        productivityMetrics: RemoteWorkProductivity = this.productivityMetrics,
        collaborationMetrics: RemoteWorkCollaboration = this.collaborationMetrics,
        infrastructureMetrics: RemoteWorkInfrastructure = this.infrastructureMetrics,
        satisfactionMetrics: RemoteWorkSatisfaction = this.satisfactionMetrics,
        rankings: Map<RankingType, RemoteWorkRankingInfo> = this.rankings,
    ) = RemoteWorkStatsDocument(
        id,
        entityId,
        baseDate,
        period,
        type,
        stats,
        industryDistribution,
        jobCategoryDistribution,
        companySizeDistribution,
        locationDistribution,
        skillDistribution,
        experienceDistribution,
        productivityMetrics,
        collaborationMetrics,
        infrastructureMetrics,
        satisfactionMetrics,
        rankings,
    )
}
