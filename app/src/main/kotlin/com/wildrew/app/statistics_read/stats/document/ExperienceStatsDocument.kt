package com.wildrew.app.statistics_read.stats.document

import com.wildrew.app.statistics_read.core.core_model.CompanySize
import com.wildrew.app.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.CommonStats
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.RankingInfo
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.RankingScore
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "experience_stats_monthly")
class ExperienceStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("range")
    val range: String, // 0-2, 3-5, 5+ 등
    @Field("stats")
    override val stats: ExperienceStats,
    @Field("industry_distribution")
    val industryDistribution: List<ExperienceIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<ExperienceJobCategory>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<ExperienceCompanySize>,
    @Field("location_distribution")
    val locationDistribution: List<ExperienceLocation>,
    @Field("skill_requirements")
    val skillRequirements: List<ExperienceSkill>,
    @Field("career_progression")
    val careerProgression: ExperienceCareerProgression,
    @Field("salary_metrics")
    val salaryMetrics: ExperienceSalaryMetrics,
    @Field("market_value")
    val marketValue: ExperienceMarketValue,
    @Field("employment_type_distribution")
    val employmentTypeDistribution: List<ExperienceEmploymentType>,
    @Field("rankings")
    override val rankings: Map<RankingType, ExperienceRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class ExperienceStats(
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
        @Field("application_rate")
        val applicationRate: Double,
        @Field("hiring_rate")
        val hiringRate: Double,
        @Field("market_demand_index")
        val marketDemandIndex: Double,
        @Field("position_level_distribution")
        val positionLevelDistribution: Map<String, Double>,
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

    data class ExperienceIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("demand_score")
        val demandScore: Double,
        @Field("career_path_potential")
        val careerPathPotential: Double,
        @Field("growth_opportunities")
        val growthOpportunities: Double,
        @Field("skill_requirements")
        val skillRequirements: List<RequiredSkill>,
    ) : Serializable {
        data class RequiredSkill(
            @Field("skill_id")
            val skillId: Long,
            @Field("name")
            val name: String,
            @Field("importance_score")
            val importanceScore: Double,
        ) : Serializable
    }

    data class ExperienceJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("required_skills_avg")
        val requiredSkillsAvg: Double,
        @Field("career_advancement_rate")
        val careerAdvancementRate: Double,
        @Field("role_complexity")
        val roleComplexity: RoleComplexity,
    ) : Serializable {
        data class RoleComplexity(
            @Field("technical_complexity")
            val technicalComplexity: Double,
            @Field("management_responsibility")
            val managementResponsibility: Double,
            @Field("decision_making_level")
            val decisionMakingLevel: Double,
        ) : Serializable
    }

    data class ExperienceCompanySize(
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("growth_opportunities")
        val growthOpportunities: Double,
        @Field("benefits_quality")
        val benefitsQuality: Double,
        @Field("work_life_balance")
        val workLifeBalance: Double,
    ) : Serializable

    data class ExperienceLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("cost_adjusted_salary")
        val costAdjustedSalary: Long,
        @Field("opportunity_density")
        val opportunityDensity: Double,
        @Field("remote_work_availability")
        val remoteWorkAvailability: Double,
    ) : Serializable

    data class ExperienceSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("requirement_frequency")
        val requirementFrequency: Double,
        @Field("mastery_expectation")
        val masteryExpectation: Double,
        @Field("salary_impact")
        val salaryImpact: Double,
        @Field("future_relevance")
        val futureRelevance: Double,
    ) : Serializable

    data class ExperienceCareerProgression(
        @Field("advancement_metrics")
        val advancementMetrics: AdvancementMetrics,
        @Field("skill_development")
        val skillDevelopment: SkillDevelopment,
        @Field("leadership_opportunities")
        val leadershipOpportunities: LeadershipOpportunities,
    ) : Serializable {
        data class AdvancementMetrics(
            @Field("promotion_timeline")
            val promotionTimeline: Double,
            @Field("role_transition_rate")
            val roleTransitionRate: Double,
            @Field("specialization_paths")
            val specializationPaths: List<String>,
        ) : Serializable

        data class SkillDevelopment(
            @Field("learning_curve")
            val learningCurve: Double,
            @Field("skill_acquisition_rate")
            val skillAcquisitionRate: Double,
            @Field("expertise_areas")
            val expertiseAreas: List<String>,
        ) : Serializable

        data class LeadershipOpportunities(
            @Field("management_track_probability")
            val managementTrackProbability: Double,
            @Field("team_size_responsibility")
            val teamSizeResponsibility: Double,
            @Field("strategic_involvement")
            val strategicInvolvement: Double,
        ) : Serializable
    }

    data class ExperienceSalaryMetrics(
        @Field("compensation_structure")
        val compensationStructure: CompensationStructure,
        @Field("salary_growth")
        val salaryGrowth: SalaryGrowth,
        @Field("market_positioning")
        val marketPositioning: MarketPositioning,
    ) : Serializable {
        data class CompensationStructure(
            @Field("base_salary_range")
            val baseSalaryRange: SalaryRange,
            @Field("bonus_potential")
            val bonusPotential: Double,
            @Field("equity_eligibility")
            val equityEligibility: Double,
        ) : Serializable

        data class SalaryRange(
            @Field("min")
            val min: Long,
            @Field("max")
            val max: Long,
            @Field("median")
            val median: Long,
        ) : Serializable

        data class SalaryGrowth(
            @Field("annual_increase_rate")
            val annualIncreaseRate: Double,
            @Field("performance_impact")
            val performanceImpact: Double,
            @Field("industry_comparison")
            val industryComparison: Double,
        ) : Serializable

        data class MarketPositioning(
            @Field("percentile_rank")
            val percentileRank: Double,
            @Field("competitive_index")
            val competitiveIndex: Double,
            @Field("market_demand_factor")
            val marketDemandFactor: Double,
        ) : Serializable
    }

    data class ExperienceMarketValue(
        @Field("demand_metrics")
        val demandMetrics: DemandMetrics,
        @Field("value_proposition")
        val valueProposition: ValueProposition,
        @Field("market_trends")
        val marketTrends: MarketTrends,
    ) : Serializable {
        data class DemandMetrics(
            @Field("current_demand")
            val currentDemand: Double,
            @Field("future_demand_forecast")
            val futureDemandForecast: Double,
            @Field("scarcity_index")
            val scarcityIndex: Double,
        ) : Serializable

        data class ValueProposition(
            @Field("unique_skills")
            val uniqueSkills: List<String>,
            @Field("industry_expertise")
            val industryExpertise: Double,
            @Field("leadership_capability")
            val leadershipCapability: Double,
        ) : Serializable

        data class MarketTrends(
            @Field("growth_sectors")
            val growthSectors: List<String>,
            @Field("emerging_roles")
            val emergingRoles: List<String>,
            @Field("skill_evolution")
            val skillEvolution: List<SkillTrend>,
        ) : Serializable {
            data class SkillTrend(
                @Field("skill_id")
                val skillId: Long,
                @Field("trend_direction")
                val trendDirection: String,
                @Field("importance_change")
                val importanceChange: Double,
            ) : Serializable
        }
    }

    data class ExperienceEmploymentType(
        @Field("type")
        val type: String,
        @Field("count")
        val count: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("distribution_ratio")
        val distributionRatio: Double,
        @Field("growth_rate")
        val growthRate: Double,
    ) : Serializable

    data class ExperienceRankingInfo(
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
        range: String = this.range,
        stats: ExperienceStats = this.stats,
        industryDistribution: List<ExperienceIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<ExperienceJobCategory> = this.jobCategoryDistribution,
        companySizeDistribution: List<ExperienceCompanySize> = this.companySizeDistribution,
        locationDistribution: List<ExperienceLocation> = this.locationDistribution,
        skillRequirements: List<ExperienceSkill> = this.skillRequirements,
        careerProgression: ExperienceCareerProgression = this.careerProgression,
        salaryMetrics: ExperienceSalaryMetrics = this.salaryMetrics,
        marketValue: ExperienceMarketValue = this.marketValue,
        employmentTypeDistribution: List<ExperienceEmploymentType> = this.employmentTypeDistribution,
        rankings: Map<RankingType, ExperienceRankingInfo> = this.rankings,
    ) = ExperienceStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        range = range,
        stats = stats,
        industryDistribution = industryDistribution,
        jobCategoryDistribution = jobCategoryDistribution,
        companySizeDistribution = companySizeDistribution,
        locationDistribution = locationDistribution,
        skillRequirements = skillRequirements,
        careerProgression = careerProgression,
        salaryMetrics = salaryMetrics,
        marketValue = marketValue,
        employmentTypeDistribution = employmentTypeDistribution,
        rankings = rankings,
    )
}
