package com.wildrew.app.statistics_read.stats.document

import com.wildrew.app.statistics_read.core.core_model.CompanySize
import com.wildrew.app.statistics_read.core.core_mongo_base.model.CommonDistribution
import com.wildrew.app.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.CommonStats
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.RankingInfo
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.RankingScore
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "contract_type_stats_monthly")
class ContractTypeStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("type")
    val type: String, // FULL_TIME, CONTRACT, FREELANCE, INTERN
    @Field("stats")
    override val stats: ContractTypeStats,
    @Field("industry_distribution")
    val industryDistribution: List<ContractTypeIndustry>,
    @Field("job_category_distribution")
    val jobCategoryDistribution: List<ContractTypeJobCategory>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<ContractTypeCompanySize>,
    @Field("location_distribution")
    val locationDistribution: List<ContractTypeLocation>,
    @Field("experience_distribution")
    val experienceDistribution: List<ContractTypeExperience>,
    @Field("skill_distribution")
    val skillDistribution: List<ContractTypeSkill>,
    @Field("compensation_metrics")
    val compensationMetrics: ContractTypeCompensation,
    @Field("employment_metrics")
    val employmentMetrics: ContractTypeEmployment,
    @Field("conversion_metrics")
    val conversionMetrics: ContractTypeConversion,
    @Field("rankings")
    override val rankings: Map<RankingType, ContractTypeRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class ContractTypeStats(
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
        @Field("market_share")
        val marketShare: Double,
        @Field("application_rate")
        val applicationRate: Double,
        @Field("contract_duration_avg")
        val contractDurationAvg: Double,
        @Field("renewal_rate")
        val renewalRate: Double,
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

    data class ContractTypeIndustry(
        @Field("industry_id")
        val industryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("utilization_rate")
        val utilizationRate: Double,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("industry_trend")
        val industryTrend: String,
    ) : Serializable

    data class ContractTypeJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("prevalence_rate")
        val prevalenceRate: Double,
        @Field("success_rate")
        val successRate: Double,
        @Field("demand_score")
        val demandScore: Double,
    ) : Serializable

    data class ContractTypeCompanySize(
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("utilization_rate")
        val utilizationRate: Double,
        @Field("satisfaction_score")
        val satisfactionScore: Double,
        @Field("retention_rate")
        val retentionRate: Double,
    ) : Serializable

    data class ContractTypeLocation(
        @Field("location_id")
        val locationId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("remote_ratio")
        val remoteRatio: Double,
        @Field("local_demand")
        val localDemand: Double,
    ) : Serializable

    data class ContractTypeExperience(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("success_rate")
        val successRate: Double,
        @Field("conversion_rate")
        val conversionRate: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class ContractTypeSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("importance_score")
        val importanceScore: Double,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("demand_score")
        val demandScore: Double,
    ) : Serializable

    data class ContractTypeCompensation(
        @Field("avg_hourly_rate")
        val avgHourlyRate: Double,
        @Field("benefits_coverage_rate")
        val benefitsCoverageRate: Double,
        @Field("salary_range")
        val salaryRange: SalaryRange,
        @Field("compensation_growth")
        val compensationGrowth: Double,
        @Field("market_rate_comparison")
        val marketRateComparison: Double,
    ) : Serializable {
        data class SalaryRange(
            @Field("min")
            val min: Long,
            @Field("max")
            val max: Long,
            @Field("median")
            val median: Long,
        ) : Serializable
    }

    data class ContractTypeEmployment(
        @Field("avg_contract_duration")
        val avgContractDuration: Double,
        @Field("renewal_probability")
        val renewalProbability: Double,
        @Field("stability_index")
        val stabilityIndex: Double,
        @Field("satisfaction_metrics")
        val satisfactionMetrics: SatisfactionMetrics,
    ) : Serializable {
        data class SatisfactionMetrics(
            @Field("employer_satisfaction")
            val employerSatisfaction: Double,
            @Field("employee_satisfaction")
            val employeeSatisfaction: Double,
            @Field("recommendation_rate")
            val recommendationRate: Double,
        ) : Serializable
    }

    data class ContractTypeConversion(
        @Field("permanent_conversion_rate")
        val permanentConversionRate: Double,
        @Field("avg_time_to_conversion")
        val avgTimeToConversion: Double,
        @Field("success_factors")
        val successFactors: List<SuccessFactor>,
        @Field("career_progression")
        val careerProgression: CareerProgression,
    ) : Serializable {
        data class SuccessFactor(
            @Field("factor")
            val factor: String,
            @Field("importance_score")
            val importanceScore: Double,
        ) : Serializable

        data class CareerProgression(
            @Field("promotion_rate")
            val promotionRate: Double,
            @Field("skill_development_rate")
            val skillDevelopmentRate: Double,
            @Field("career_growth_score")
            val careerGrowthScore: Double,
        ) : Serializable
    }

    data class ContractTypeRankingInfo(
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
        type: String = this.type,
        stats: ContractTypeStats = this.stats,
        industryDistribution: List<ContractTypeIndustry> = this.industryDistribution,
        jobCategoryDistribution: List<ContractTypeJobCategory> = this.jobCategoryDistribution,
        companySizeDistribution: List<ContractTypeCompanySize> = this.companySizeDistribution,
        locationDistribution: List<ContractTypeLocation> = this.locationDistribution,
        experienceDistribution: List<ContractTypeExperience> = this.experienceDistribution,
        skillDistribution: List<ContractTypeSkill> = this.skillDistribution,
        compensationMetrics: ContractTypeCompensation = this.compensationMetrics,
        employmentMetrics: ContractTypeEmployment = this.employmentMetrics,
        conversionMetrics: ContractTypeConversion = this.conversionMetrics,
        rankings: Map<RankingType, ContractTypeRankingInfo> = this.rankings,
    ) = ContractTypeStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        type = type,
        stats = stats,
        industryDistribution = industryDistribution,
        jobCategoryDistribution = jobCategoryDistribution,
        companySizeDistribution = companySizeDistribution,
        locationDistribution = locationDistribution,
        experienceDistribution = experienceDistribution,
        skillDistribution = skillDistribution,
        compensationMetrics = compensationMetrics,
        employmentMetrics = employmentMetrics,
        conversionMetrics = conversionMetrics,
        rankings = rankings,
    )
}
