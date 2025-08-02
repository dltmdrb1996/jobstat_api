package com.wildrew.jobstat.statistics_read.rankings.model.rankingtype

import com.wildrew.jobstat.statistics_read.stats.registry.StatsType

enum class RankingType(
    val fieldName: String,
) {
    // --- 직무 (Job Category) ---
    JOB_CATEGORY_POSTING_COUNT("jobPostingCount"), // 직무 채용공고 수
    JOB_CATEGORY_SALARY("jobAvgSalary"), // 직무 평균 급여
    JOB_CATEGORY_GROWTH("jobCategoryGrowth"), // 직무 공고 증가율
    JOB_CATEGORY_APPLICATION_RATE("jobCategoryApplicationRate"), // 직무 지원율
    JOB_CATEGORY_SKILL("jobCategorySkill"), // 직무별 기술 선호도

    // --- 업종 (Industry) ---
    INDUSTRY_POSTING_COUNT("industryPostingCount"), // 업종별 채용공고 수
    INDUSTRY_SALARY("industryAvgSalary"), // 업종별 평균 급여
    INDUSTRY_GROWTH("industryGrowth"), // 채용 성장 TOP 산업
    INDUSTRY_SKILL("industrySkill"), // 업종별 기술 선호도

    // --- 스킬 (Skill) ---
    SKILL_POSTING_COUNT("skillPostingCount"), // 기술스택별 채용공고 수
    SKILL_SALARY("skillSalary"), // 기술스택별 급여
    SKILL_GROWTH("skillGrowth"), // 기술스택별 성장률
    SKILL_COMPETITION_RATE("skillCompetitionRate"), // 기술스택별 경쟁률

    // --- 회사 (Company) ---
    COMPANY_HIRING_VOLUME("companyHiringVolume"), // 채용 규모 TOP 기업
    COMPANY_SALARY("companySalary"), // 기업별 평균 급여
    COMPANY_GROWTH("companyGrowth"), // 기업별 성장률
    COMPANY_RETENTION_RATE("companyRetentionRate"), // 기업별 이직률
    COMPANY_BENEFIT_COUNT("benefitCount"), // 복리후생 수

    // --- 회사 규모 (Company Size) ---
    COMPANY_SIZE_POSTING_COUNT("companySizePostingCount"), // 회사규모별 채용공고 수
    COMPANY_SIZE_SALARY("companySizeSalary"), // 회사규모별 급여
    COMPANY_SIZE_BENEFIT("companySizeBenefit"), // 회사규모별 복리후생 제공 비율
    COMPANY_SIZE_SKILL_DEMAND("companySizeSkillDemand"), // 회사규모별 기술 선호도
    COMPANY_SIZE_EDUCATION("companySizeEducation"), // 회사규모별 학력 요구 비율

    // --- 지역 (Location) ---
    LOCATION_POSTING_COUNT("locationPostingCount"), // 지역별 채용공고 수
    LOCATION_SALARY("locationSalary"), // 지역별 급여
    LOCATION_GROWTH("locationGrowth"), // 지역별 채용공고 증가율

    // --- 학력 (Education) ---
    EDUCATION_POSTING_COUNT("educationPostingCount"), // 학력별 채용공고 수
    EDUCATION_SALARY("educationSalary"), // 학력별 평균 급여

    // --- 경력 (Experience) ---
    EXPERIENCE_POSTING_COUNT("experiencePostingCount"), // 경력별 채용공고 수
    EXPERIENCE_SALARY("experienceSalary"), // 경력별 평균 급여

    // --- 자격증 (Certification) ---
    CERTIFICATION_POSTING_COUNT("certificationPostingCount"), // 자격증별 채용공고 수
    CERTIFICATION_SALARY("certificationSalary"), // 자격증별 급여
    CERTIFICATION_GROWTH("certificationGrowth"), // 자격증별 성장률

    // --- 복리후생 (Benefit) ---
    BENEFIT_POSTING_COUNT("benefitPostingCount"), // 복리후생별 채용공고 수

    // --- 계약 형태 (Contract Type) ---
    CONTRACT_TYPE_POSTING_COUNT("contractTypePostingCount"), // 계약 유형별 채용공고 수

    // --- 원격 근무 (Remote Work) ---
    REMOTE_WORK_TYPE_POSTING_COUNT("remoteWorkTypePostingCount"), // 원격 근무 형태별 채용공고 수
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
