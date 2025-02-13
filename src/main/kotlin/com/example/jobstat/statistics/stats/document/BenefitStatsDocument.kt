package com.example.jobstat.statistics.stats.document

import com.example.jobstat.core.base.mongo.CommonDistribution
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.mongo.stats.CommonStats
import com.example.jobstat.core.base.mongo.stats.RankingInfo
import com.example.jobstat.core.base.mongo.stats.RankingScore
import com.example.jobstat.core.state.CompanySize
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "benefit_stats_monthly")
class BenefitStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: BenefitStats,
    @Field("industry_distribution")
    val industryDistribution: List<BenefitIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<BenefitJobCategory>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<BenefitCompanySize>,
    @Field("location_distribution")
    val locationDistribution: List<BenefitLocation>,
    @Field("experience_distribution")
    val experienceDistribution: List<BenefitExperience>,
    @Field("compensation_impact")
    val compensationImpact: BenefitCompensationImpact,
    @Field("employee_satisfaction")
    val employeeSatisfaction: BenefitSatisfactionMetrics,
    @Field("cost_metrics")
    val costMetrics: BenefitCostMetrics,
    @Field("rankings")
    override val rankings: Map<RankingType, BenefitRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class BenefitStats(
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
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("company_count")
        val companyCount: Int,
        @Field("employee_coverage")
        val employeeCoverage: Double,
        @Field("satisfaction_score")
        val satisfactionScore: Double,
        @Field("retention_impact")
        val retentionImpact: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class BenefitIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("provision_count")
        val provisionCount: Int,
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("avg_salary_with_benefit")
        val avgSalaryWithBenefit: Long,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("industry_standard_score")
        val industryStandardScore: Double,
    ) : Serializable

    data class BenefitJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("provision_count")
        val provisionCount: Int,
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("importance_score")
        val importanceScore: Double,
        @Field("satisfaction_score")
        val satisfactionScore: Double,
        @Field("retention_impact")
        val retentionImpact: Double,
    ) : Serializable

    data class BenefitCompanySize(
        @Field("size")
        val size: CompanySize,
        @Field("provision_count")
        val provisionCount: Int,
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("avg_cost_per_employee")
        val avgCostPerEmployee: Long,
        @Field("satisfaction_score")
        val satisfactionScore: Double,
        @Field("market_competitiveness")
        val marketCompetitiveness: Double,
    ) : Serializable

    data class BenefitLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("provision_count")
        val provisionCount: Int,
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("local_importance")
        val localImportance: Double,
        @Field("cost_of_living_adjusted_value")
        val costOfLivingAdjustedValue: Double,
    ) : Serializable

    data class BenefitExperience(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("provision_rate")
        val provisionRate: Double,
        @Field("importance_score")
        val importanceScore: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class BenefitCompensationImpact(
        @Field("monetary_value")
        val monetaryValue: Long,
        @Field("salary_equivalent")
        val salaryEquivalent: Double,
        @Field("tax_impact")
        val taxImpact: Double,
        @Field("total_compensation_ratio")
        val totalCompensationRatio: Double,
        @Field("market_value_comparison")
        val marketValueComparison: Double,
    ) : Serializable

    data class BenefitSatisfactionMetrics(
        @Field("overall_satisfaction")
        val overallSatisfaction: Double,
        @Field("utilization_rate")
        val utilizationRate: Double,
        @Field("perceived_value")
        val perceivedValue: Double,
        @Field("preference_rating")
        val preferenceRating: Double,
        @Field("retention_correlation")
        val retentionCorrelation: Double,
        @Field("feedback_analysis")
        val feedbackAnalysis: FeedbackMetrics,
    ) : Serializable {
        data class FeedbackMetrics(
            @Field("positive_feedback_ratio")
            val positiveFeedbackRatio: Double,
            @Field("improvement_suggestions")
            val improvementSuggestions: List<String>,
            @Field("satisfaction_factors")
            val satisfactionFactors: Map<String, Double>,
        ) : Serializable
    }

    data class BenefitCostMetrics(
        @Field("avg_cost_per_employee")
        val avgCostPerEmployee: Long,
        @Field("total_investment")
        val totalInvestment: Long,
        @Field("roi_metrics")
        val roiMetrics: RoiMetrics,
        @Field("cost_trend")
        val costTrend: CostTrend,
    ) : Serializable {
        data class RoiMetrics(
            @Field("retention_roi")
            val retentionRoi: Double,
            @Field("productivity_impact")
            val productivityImpact: Double,
            @Field("cost_benefit_ratio")
            val costBenefitRatio: Double,
        ) : Serializable

        data class CostTrend(
            @Field("year_over_year_change")
            val yearOverYearChange: Double,
            @Field("cost_efficiency_score")
            val costEfficiencyScore: Double,
            @Field("market_cost_comparison")
            val marketCostComparison: Double,
        ) : Serializable
    }

    data class BenefitRankingInfo(
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
        TODO("아직 구현되지 않음")
    }

    fun copy(
        entityId: Long = this.entityId,
        baseDate: String = this.baseDate,
        period: SnapshotPeriod = this.period,
        name: String = this.name,
        stats: BenefitStats = this.stats,
        industryDistribution: List<BenefitIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<BenefitJobCategory> = this.jobCategoryDistribution,
        companySizeDistribution: List<BenefitCompanySize> = this.companySizeDistribution,
        locationDistribution: List<BenefitLocation> = this.locationDistribution,
        experienceDistribution: List<BenefitExperience> = this.experienceDistribution,
        compensationImpact: BenefitCompensationImpact = this.compensationImpact,
        employeeSatisfaction: BenefitSatisfactionMetrics = this.employeeSatisfaction,
        costMetrics: BenefitCostMetrics = this.costMetrics,
        rankings: Map<RankingType, BenefitRankingInfo> = this.rankings,
    ) = BenefitStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        name = name,
        stats = stats,
        industryDistribution = industryDistribution,
        jobCategoryDistribution = jobCategoryDistribution,
        companySizeDistribution = companySizeDistribution,
        locationDistribution = locationDistribution,
        experienceDistribution = experienceDistribution,
        compensationImpact = compensationImpact,
        employeeSatisfaction = employeeSatisfaction,
        costMetrics = costMetrics,
        rankings = rankings,
    )
}
