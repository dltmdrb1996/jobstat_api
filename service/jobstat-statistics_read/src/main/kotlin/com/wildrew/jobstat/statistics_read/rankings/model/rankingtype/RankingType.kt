package com.wildrew.jobstat.statistics_read.rankings.model.rankingtype

import com.wildrew.jobstat.statistics_read.stats.registry.StatsType

enum class RankingType(
    val fieldName: String,
) {
    JOB_CATEGORY_POSTING_COUNT("jobPostingCount"),
    JOB_CATEGORY_SALARY("jobAvgSalary"),
    JOB_CATEGORY_GROWTH("jobCategoryGrowth"),
    JOB_CATEGORY_APPLICATION_RATE("jobCategoryApplicationRate"),
    JOB_CATEGORY_SKILL("jobCategorySkill"),

    INDUSTRY_POSTING_COUNT("industryPostingCount"),
    INDUSTRY_SALARY("industryAvgSalary"),
    INDUSTRY_GROWTH("industryGrowth"),
    INDUSTRY_SKILL("industrySkill"),

    SKILL_POSTING_COUNT("skillPostingCount"),
    SKILL_SALARY("skillSalary"),
    SKILL_GROWTH("skillGrowth"),
    SKILL_COMPETITION_RATE("skillCompetitionRate"),

    COMPANY_HIRING_VOLUME("companyHiringVolume"),
    COMPANY_SALARY("companySalary"),
    COMPANY_GROWTH("companyGrowth"),
    COMPANY_RETENTION_RATE("companyRetentionRate"),
    COMPANY_BENEFIT_COUNT("benefitCount"),

    COMPANY_SIZE_POSTING_COUNT("companySizePostingCount"),
    COMPANY_SIZE_SALARY("companySizeSalary"),
    COMPANY_SIZE_BENEFIT("companySizeBenefit"),
    COMPANY_SIZE_SKILL_DEMAND("companySizeSkillDemand"),
    COMPANY_SIZE_EDUCATION("companySizeEducation"),

    LOCATION_POSTING_COUNT("locationPostingCount"),
    LOCATION_SALARY("locationSalary"),
    LOCATION_GROWTH("locationGrowth"),

    EDUCATION_POSTING_COUNT("educationPostingCount"),
    EDUCATION_SALARY("educationSalary"),

    EXPERIENCE_POSTING_COUNT("experiencePostingCount"),
    EXPERIENCE_SALARY("experienceSalary"),

    CERTIFICATION_POSTING_COUNT("certificationPostingCount"),
    CERTIFICATION_SALARY("certificationSalary"),
    CERTIFICATION_GROWTH("certificationGrowth"),

    BENEFIT_POSTING_COUNT("benefitPostingCount"),

    CONTRACT_TYPE_POSTING_COUNT("contractTypePostingCount"),

    REMOTE_WORK_TYPE_POSTING_COUNT("remoteWorkTypePostingCount"),
    ;

    companion object {
        fun fromField(fieldName: String) =
            values().find { it.fieldName == fieldName }
                ?: throw IllegalArgumentException("Invalid field name: $fieldName")
    }
}

fun RankingType.toStatsType(): StatsType =
    when (this) {
        RankingType.SKILL_SALARY,
        RankingType.SKILL_POSTING_COUNT,
        RankingType.SKILL_GROWTH,
        RankingType.SKILL_COMPETITION_RATE,
        -> StatsType.SKILL

        RankingType.JOB_CATEGORY_SKILL,
        RankingType.JOB_CATEGORY_APPLICATION_RATE,
        RankingType.JOB_CATEGORY_GROWTH,
        RankingType.JOB_CATEGORY_SALARY,
        RankingType.JOB_CATEGORY_POSTING_COUNT,
        -> StatsType.JOB_CATEGORY

        RankingType.INDUSTRY_SKILL,
        RankingType.INDUSTRY_GROWTH,
        RankingType.INDUSTRY_SALARY,
        RankingType.INDUSTRY_POSTING_COUNT,
        -> StatsType.INDUSTRY

        RankingType.CERTIFICATION_POSTING_COUNT,
        RankingType.CERTIFICATION_SALARY,
        RankingType.CERTIFICATION_GROWTH,
        -> StatsType.CERTIFICATION

        RankingType.LOCATION_SALARY,
        RankingType.LOCATION_POSTING_COUNT,
        RankingType.LOCATION_GROWTH,
        -> StatsType.LOCATION

        RankingType.COMPANY_SIZE_SKILL_DEMAND,
        RankingType.COMPANY_SIZE_SALARY,
        RankingType.COMPANY_SIZE_BENEFIT,
        RankingType.COMPANY_SIZE_POSTING_COUNT,
        RankingType.COMPANY_SIZE_EDUCATION,
        -> StatsType.COMPANY_SIZE

        RankingType.COMPANY_HIRING_VOLUME,
        RankingType.COMPANY_SALARY,
        RankingType.COMPANY_GROWTH,
        RankingType.COMPANY_RETENTION_RATE,
        RankingType.COMPANY_BENEFIT_COUNT,
        -> StatsType.COMPANY

        RankingType.BENEFIT_POSTING_COUNT -> StatsType.BENEFIT

        RankingType.EDUCATION_SALARY,
        RankingType.EDUCATION_POSTING_COUNT,
        -> StatsType.EDUCATION

        RankingType.EXPERIENCE_POSTING_COUNT,
        RankingType.EXPERIENCE_SALARY,
        -> StatsType.EXPERIENCE

        RankingType.CONTRACT_TYPE_POSTING_COUNT -> StatsType.CONTRACT_TYPE

        RankingType.REMOTE_WORK_TYPE_POSTING_COUNT -> StatsType.REMOTE_WORK_TYPE
    }
