package com.example.jobstat.statistics.stats.document

import com.example.jobstat.core.base.mongo.CommonDistribution
import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.mongo.stats.CommonStats
import com.example.jobstat.core.base.mongo.stats.RankingInfo
import com.example.jobstat.core.base.mongo.stats.RankingScore
import com.example.jobstat.core.state.CompanySize
import com.example.jobstat.statistics.rankings.model.RankingType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@CompoundIndexes(
    CompoundIndex(
        name = "skill_posting_lookup_idx",
        def = "{'skills.posting_count': -1}",
    ),
)
@Document(collection = "industry_stats_monthly")
class IndustryStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: IndustryStats,
    @Field("job_categories")
    val jobCategories: List<IndustryJobCategory>,
    @Field("skills")
    val skills: List<IndustrySkill>,
    @Field("companies")
    val companies: List<IndustryCompany>,
    @Field("experience_distribution")
    val experienceDistribution: List<ExperienceDistribution>,
    @Field("education_distribution")
    val educationDistribution: List<EducationDistribution>,
    @Field("location_distribution")
    val locationDistribution: List<LocationDistribution>,
    @Field("salary_range_distribution")
    val salaryRangeDistribution: List<SalaryRangeDistribution>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<CompanySizeDistribution>,
    @Field("remote_work_ratio")
    val remoteWorkRatio: Double,
    @Field("contract_type_distribution")
    val contractTypeDistribution: List<ContractTypeDistribution>,
    @Field("rankings")
    override val rankings: Map<RankingType, IndustryRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class IndustryStats(
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
        @Field("market_size")
        val marketSize: Long,
        @Field("employment_rate")
        val employmentRate: Double,
        @Field("turnover_rate")
        val turnoverRate: Double,
        @Field("avg_company_size")
        val avgCompanySize: Double,
        @Field("startup_ratio")
        val startupRatio: Double,
        @Field("enterprise_ratio")
        val enterpriseRatio: Double,
        @Field("avg_experience_requirement")
        val avgExperienceRequirement: Double,
        @Field("application_competition_rate")
        val applicationCompetitionRate: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class IndustryJobCategory(
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
        @Field("demand_score")
        val demandScore: Double,
        @Field("skill_diversity")
        val skillDiversity: Double,
        @Field("remote_work_ratio")
        val remoteWorkRatio: Double,
    ) : Serializable

    data class IndustrySkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("posting_count")
        val postingCount: Int,
        @Field("importance_score")
        val importanceScore: Double,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("required_ratio")
        val requiredRatio: Double,
        @Field("preferred_ratio")
        val preferredRatio: Double,
    ) : Serializable

    data class IndustryCompany(
        @Field("company_id")
        val companyId: Long,
        @Field("name")
        val name: String,
        @Field("size")
        val size: CompanySize,
        @Field("posting_count")
        val postingCount: Int,
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("employee_count")
        val employeeCount: Int,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("market_share")
        val marketShare: Double,
    ) : Serializable

    data class ExperienceDistribution(
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
        @Field("demand_score")
        val demandScore: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class EducationDistribution(
        @Field("level")
        val level: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("growth_rate")
        val growthRate: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class LocationDistribution(
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
        @Field("growth_rate")
        val growthRate: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class SalaryRangeDistribution(
        @Field("range")
        val range: String,
        @Field("min_salary")
        val minSalary: Long,
        @Field("max_salary")
        val maxSalary: Long,
        @Field("count")
        override val count: Int,
        @Field("ratio")
        override val ratio: Double,
        @Field("avg_salary")
        override val avgSalary: Long?,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CompanySizeDistribution(
        @Field("size")
        val size: CompanySize,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("growth_rate")
        val growthRate: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

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

    data class IndustryRankingInfo(
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
        stats: IndustryStats = this.stats,
        jobCategories: List<IndustryJobCategory> = this.jobCategories,
        skills: List<IndustrySkill> = this.skills,
        companies: List<IndustryCompany> = this.companies,
        experienceDistribution: List<ExperienceDistribution> = this.experienceDistribution,
        educationDistribution: List<EducationDistribution> = this.educationDistribution,
        locationDistribution: List<LocationDistribution> = this.locationDistribution,
        salaryRangeDistribution: List<SalaryRangeDistribution> = this.salaryRangeDistribution,
        companySizeDistribution: List<CompanySizeDistribution> = this.companySizeDistribution,
        remoteWorkRatio: Double = this.remoteWorkRatio,
        contractTypeDistribution: List<ContractTypeDistribution> = this.contractTypeDistribution,
        rankings: Map<RankingType, IndustryRankingInfo> = this.rankings,
    ) = IndustryStatsDocument(
        id,
        entityId,
        baseDate,
        period,
        name,
        stats,
        jobCategories,
        skills,
        companies,
        experienceDistribution,
        educationDistribution,
        locationDistribution,
        salaryRangeDistribution,
        companySizeDistribution,
        remoteWorkRatio,
        contractTypeDistribution,
        rankings,
    )
}
