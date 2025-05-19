package com.example.jobstat.statistics_read.stats.document

import com.example.jobstat.core.core_mongo_base.model.CommonDistribution
import com.example.jobstat.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.core.core_mongo_base.model.stats.BaseStatsDocument
import com.example.jobstat.core.core_mongo_base.model.stats.CommonStats
import com.example.jobstat.core.core_mongo_base.model.stats.RankingInfo
import com.example.jobstat.core.core_mongo_base.model.stats.RankingScore
import com.example.jobstat.core.core_model.CompanySize
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@CompoundIndexes(
    CompoundIndex(
        name = "skill_posting_lookup_idx",
        def = "{'skills.posting_count': -1}",
    ),
)
@Document(collection = "job_category_stats_monthly")
class JobCategoryStatsDocument(
    id: String? = null,
    @Field("entity_id")
    override val entityId: Long,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("name")
    val name: String,
    @Field("stats")
    override val stats: JobCategoryStats,
    @Field("skills")
    val skills: List<JobCategorySkill>,
    @Field("certifications")
    val certifications: List<JobCategoryCertification>,
    @Field("experience_distribution")
    val experienceDistribution: List<ExperienceDistribution>,
    @Field("education_distribution")
    val educationDistribution: List<EducationDistribution>,
    @Field("salary_range_distribution")
    val salaryRangeDistribution: List<SalaryRangeDistribution>,
    @Field("company_size_distribution")
    val companySizeDistribution: List<CompanySizeDistribution>,
    @Field("industry_distribution")
    val industryDistribution: List<IndustryDistribution>,
    @Field("location_distribution")
    val locationDistribution: List<LocationDistribution>,
    @Field("benefits_distribution")
    val benefitsDistribution: List<BenefitDistribution>,
    @Field("remote_work_ratio")
    val remoteWorkRatio: Double,
    @Field("contract_type_distribution")
    val contractTypeDistribution: List<ContractTypeDistribution>,
    @Field("transition_paths")
    val transitionPaths: List<CareerTransitionPath>,
    @Field("rankings")
    override val rankings: Map<RankingType, JobCategoryRankingInfo>,
) : BaseStatsDocument(id, baseDate, period, entityId, stats, rankings) {
    data class JobCategoryStats(
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
        @Field("application_count")
        val applicationCount: Int,
        @Field("avg_experience_requirement")
        val avgExperienceRequirement: Double,
        @Field("competition_rate")
        val competitionRate: Double,
        @Field("avg_time_to_fill")
        val avgTimeToFill: Double,
        @Field("entry_level_ratio")
        val entryLevelRatio: Double,
        @Field("career_level_ratio")
        val careerLevelRatio: Double,
        @Field("required_skill_count_avg")
        val requiredSkillCountAvg: Double,
        @Field("certification_requirement_ratio")
        val certificationRequirementRatio: Double,
        @Field("remote_work_availability")
        val remoteWorkAvailability: Double,
        @Field("market_competitiveness")
        val marketCompetitiveness: Double,
    ) : CommonStats(
            postingCount,
            activePostingCount,
            avgSalary,
            growthRate,
            yearOverYearGrowth,
            monthOverMonthChange,
            demandTrend,
        )

    data class JobCategorySkill(
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
        @Field("emerging_skill")
        val emergingSkill: Boolean,
//        @Field("skills_correlation")
//        val skillsCorrelation: Map<Long, Double>,
    )

    data class JobCategoryCertification(
        @Field("certification_id")
        val certificationId: Long,
        @Field("name")
        val name: String,
        @Field("required_count")
        val requiredCount: Int,
        @Field("preferred_count")
        val preferredCount: Int,
        @Field("salary_premium")
        val salaryPremium: Double,
        @Field("demand_score")
        val demandScore: Double,
    )

    data class ExperienceDistribution(
        @Field("range")
        val range: String,
        @Field("count")
        override val count: Int,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("ratio")
        override val ratio: Double,
        @Field("competition_rate")
        val competitionRate: Double,
        @Field("skill_requirement_count_avg")
        val skillRequirementCountAvg: Double,
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
        @Field("preferred_ratio")
        val preferredRatio: Double,
        @Field("required_ratio")
        val requiredRatio: Double,
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
        @Field("experience_requirement_avg")
        val experienceRequirementAvg: Double,
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

    // JobCategoryStatsDocument의 나머지 data class들
    data class IndustryDistribution(
        @Field("industry_id")
        val industryId: Long,
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
        @Field("market_share")
        val marketShare: Double,
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
        @Field("remote_work_ratio")
        val remoteWorkRatio: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class BenefitDistribution(
        @Field("benefit_id")
        val benefitId: Long,
        @Field("name")
        val name: String,
        @Field("count")
        override val count: Int,
        @Field("ratio")
        override val ratio: Double,
        @Field("avg_salary")
        override val avgSalary: Long?,
        @Field("satisfaction_score")
        val satisfactionScore: Double,
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
        @Field("growth_rate")
        val growthRate: Double,
    ) : CommonDistribution(count, ratio, avgSalary)

    data class CareerTransitionPath(
        @Field("to_job_category_id")
        val toJobCategoryId: Long,
        @Field("name")
        val name: String,
        @Field("transition_count")
        val transitionCount: Int,
        @Field("success_rate")
        val successRate: Double,
        @Field("avg_salary_change")
        val avgSalaryChange: Double,
        @Field("required_skill_gap")
        val requiredSkillGap: List<SkillGap>,
    ) {
        data class SkillGap(
            @Field("skill_id")
            val skillId: Long,
            @Field("name")
            val name: String,
            @Field("importance_score")
            val importanceScore: Double,
        )
    }

    data class JobCategoryRankingInfo(
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
        stats: JobCategoryStats = this.stats,
        skills: List<JobCategorySkill> = this.skills,
        certifications: List<JobCategoryCertification> = this.certifications,
        experienceDistribution: List<ExperienceDistribution> = this.experienceDistribution,
        educationDistribution: List<EducationDistribution> = this.educationDistribution,
        salaryRangeDistribution: List<SalaryRangeDistribution> = this.salaryRangeDistribution,
        companySizeDistribution: List<CompanySizeDistribution> = this.companySizeDistribution,
        industryDistribution: List<IndustryDistribution> = this.industryDistribution,
        locationDistribution: List<LocationDistribution> = this.locationDistribution,
        benefitsDistribution: List<BenefitDistribution> = this.benefitsDistribution,
        remoteWorkRatio: Double = this.remoteWorkRatio,
        contractTypeDistribution: List<ContractTypeDistribution> = this.contractTypeDistribution,
        transitionPaths: List<CareerTransitionPath> = this.transitionPaths,
        rankings: Map<RankingType, JobCategoryRankingInfo> = this.rankings,
    ) = JobCategoryStatsDocument(
        id = this.id,
        entityId = entityId,
        baseDate = baseDate,
        period = period,
        name = name,
        stats = stats,
        skills = skills,
        certifications = certifications,
        experienceDistribution = experienceDistribution,
        educationDistribution = educationDistribution,
        salaryRangeDistribution = salaryRangeDistribution,
        companySizeDistribution = companySizeDistribution,
        industryDistribution = industryDistribution,
        locationDistribution = locationDistribution,
        benefitsDistribution = benefitsDistribution,
        remoteWorkRatio = remoteWorkRatio,
        contractTypeDistribution = contractTypeDistribution,
        transitionPaths = transitionPaths,
        rankings = rankings,
    )
}
