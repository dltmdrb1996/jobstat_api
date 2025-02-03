package com.example.jobstat.statistics.stats.model

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.mongo.stats.CommonStats
import com.example.jobstat.core.base.mongo.stats.RankingInfo
import com.example.jobstat.core.base.mongo.stats.RankingScore
import com.example.jobstat.core.state.CompanySize
import com.example.jobstat.statistics.rankings.model.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "education_stats_monthly")
class EducationStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("level")
    val level: String, // HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE, OTHER
    @Field("stats")
    override val stats: EducationStats,
    @Field("industry_distribution")
    val industryDistribution: List<EducationIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<EducationJobCategory>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<EducationCompanySize>,
    @Field("location_distribution")
    val locationDistribution: List<EducationLocation>,
    @Field("skill_requirements")
    val skillRequirements: List<EducationSkill>,
    @Field("career_metrics")
    val careerMetrics: EducationCareerMetrics,
    @Field("roi_metrics")
    val roiMetrics: EducationRoiMetrics,
    @Field("market_demand")
    val marketDemand: EducationMarketDemand,
    @Field("rankings")
    override val rankings: Map<RankingType, EducationRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class EducationStats(
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
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("preference_rate")
        val preferenceRate: Double,
        @Field("degree_holder_employment_rate")
        val degreeHolderEmploymentRate: Double,
        @Field("career_advancement_rate")
        val careerAdvancementRate: Double,
        @Field("market_value_index")
        val marketValueIndex: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class EducationIndustry(
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
        @Field("career_growth_rate")
        val careerGrowthRate: Double,
        @Field("value_recognition")
        val valueRecognition: Double,
        @Field("industry_relevance")
        val industryRelevance: Double,
    ) : Serializable

    data class EducationJobCategory(
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
        @Field("career_potential")
        val careerPotential: Double,
        @Field("skill_match_rate")
        val skillMatchRate: Double,
    ) : Serializable

    data class EducationCompanySize(
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("growth_opportunities")
        val growthOpportunities: Double,
        @Field("hiring_preference")
        val hiringPreference: Double,
    ) : Serializable

    data class EducationLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("job_market_fit")
        val jobMarketFit: Double,
        @Field("opportunity_index")
        val opportunityIndex: Double,
    ) : Serializable

    data class EducationSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("requirement_rate")
        val requirementRate: Double,
        @Field("importance_score")
        val importanceScore: Double,
        @Field("skill_gap")
        val skillGap: Double,
        @Field("learning_curve")
        val learningCurve: Double,
    ) : Serializable

    data class EducationCareerMetrics(
        @Field("career_progression")
        val careerProgression: CareerProgression,
        @Field("employment_outcomes")
        val employmentOutcomes: EmploymentOutcomes,
        @Field("skill_utilization")
        val skillUtilization: SkillUtilization,
    ) : Serializable {
        data class CareerProgression(
            @Field("promotion_rate")
            val promotionRate: Double,
            @Field("management_track_ratio")
            val managementTrackRatio: Double,
            @Field("specialization_opportunities")
            val specializationOpportunities: Double,
        ) : Serializable

        data class EmploymentOutcomes(
            @Field("time_to_employment")
            val timeToEmployment: Double,
            @Field("job_satisfaction")
            val jobSatisfaction: Double,
            @Field("career_stability")
            val careerStability: Double,
        ) : Serializable

        data class SkillUtilization(
            @Field("knowledge_application")
            val knowledgeApplication: Double,
            @Field("skill_relevance")
            val skillRelevance: Double,
            @Field("continuous_learning")
            val continuousLearning: Double,
        ) : Serializable
    }

    data class EducationRoiMetrics(
        @Field("financial_metrics")
        val financialMetrics: FinancialMetrics,
        @Field("career_value")
        val careerValue: CareerValue,
        @Field("market_performance")
        val marketPerformance: MarketPerformance,
    ) : Serializable {
        data class FinancialMetrics(
            @Field("education_cost")
            val educationCost: Long,
            @Field("salary_premium")
            val salaryPremium: Double,
            @Field("break_even_time")
            val breakEvenTime: Double,
        ) : Serializable

        data class CareerValue(
            @Field("career_opportunities")
            val careerOpportunities: Double,
            @Field("skill_development")
            val skillDevelopment: Double,
            @Field("network_value")
            val networkValue: Double,
        ) : Serializable

        data class MarketPerformance(
            @Field("market_demand")
            val marketDemand: Double,
            @Field("competitive_advantage")
            val competitiveAdvantage: Double,
            @Field("future_prospects")
            val futureProspects: Double,
        ) : Serializable
    }

    data class EducationMarketDemand(
        @Field("current_demand")
        val currentDemand: CurrentDemand,
        @Field("future_outlook")
        val futureOutlook: FutureOutlook,
        @Field("competitive_landscape")
        val competitiveLandscape: CompetitiveLandscape,
    ) : Serializable {
        data class CurrentDemand(
            @Field("demand_score")
            val demandScore: Double,
            @Field("market_saturation")
            val marketSaturation: Double,
            @Field("requirement_trend")
            val requirementTrend: String,
        ) : Serializable

        data class FutureOutlook(
            @Field("growth_projection")
            val growthProjection: Double,
            @Field("emerging_opportunities")
            val emergingOpportunities: List<String>,
            @Field("risk_factors")
            val riskFactors: List<String>,
        ) : Serializable

        data class CompetitiveLandscape(
            @Field("competition_level")
            val competitionLevel: Double,
            @Field("market_position")
            val marketPosition: Double,
            @Field("differentiation_factors")
            val differentiationFactors: List<String>,
        ) : Serializable
    }

    data class EducationRankingInfo(
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
        level: String = this.level,
        stats: EducationStats = this.stats,
        industryDistribution: List<EducationIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<EducationJobCategory> = this.jobCategoryDistribution,
        companySizeDistribution: List<EducationCompanySize> = this.companySizeDistribution,
        locationDistribution: List<EducationLocation> = this.locationDistribution,
        skillRequirements: List<EducationSkill> = this.skillRequirements,
        careerMetrics: EducationCareerMetrics = this.careerMetrics,
        roiMetrics: EducationRoiMetrics = this.roiMetrics,
        marketDemand: EducationMarketDemand = this.marketDemand,
        rankings: Map<RankingType, EducationRankingInfo> = this.rankings,
    ) = EducationStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        level = level,
        stats = stats,
        industryDistribution = industryDistribution,
        jobCategoryDistribution = jobCategoryDistribution,
        companySizeDistribution = companySizeDistribution,
        locationDistribution = locationDistribution,
        skillRequirements = skillRequirements,
        careerMetrics = careerMetrics,
        roiMetrics = roiMetrics,
        marketDemand = marketDemand,
        rankings = rankings,
    )
}
