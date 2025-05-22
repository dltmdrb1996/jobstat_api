package com.wildrew.app.statistics_read.rankings.model.rankingtype

import com.wildrew.app.statistics_read.stats.registry.StatsType
import java.io.Serializable

enum class RankingType(
    val fieldName: String,
) : Serializable {
    // 스킬 랭킹
    SKILL_SALARY("skillSalary"), // 기술스택별 급여
    SKILL_POSTING_COUNT("skillPostingCount"), // 기술스택별 채용공고 수
    SKILL_GROWTH("skillGrowth"), // 기술스택별 성장률
    SKILL_COMPETITION_RATE("skillCompetitionRate"), // 기술스택별 경쟁률

    // 직무별/업종별 랭킹
    JOB_CATEGORY_SKILL("jobCategorySkill"), // 직무별 기술 선호도
    INDUSTRY_SKILL("industrySkill"), // 업종별 기술 선호도

    // 직무랭킹
    JOB_CATEGORY_APPLICATION_RATE("jobCategoryApplicationRate"), // 직무 지원율
    JOB_CATEGORY_GROWTH("jobCategoryGrowth"), // 직무 공고 증가율
    JOB_CATEGORY_SALARY("jobAvgSalary"), // 직무 평균 급여
    JOB_CATEGORY_POSTING_COUNT("jobPostingCount"), // 직무 채용공고 수

    // 업종랭킹
    INDUSTRY_GROWTH("industryGrowth"), // 채용 성장 TOP 산업
    INDUSTRY_SALARY("industryAvgSalary"), // 업종별 평균 급여

    // 자격증 랭킹
    CERTIFICATION_POSTING_COUNT("certificationPostingCount"), // 자격증별 채용공고 수
    CERTIFICATION_SALARY("certificationSalary"), // 자격증별 급여
    CERTIFICATION_GROWTH("certificationGrowth"), // 자격증별 성장률

    // 지역 관련
    LOCATION_SALARY("locationSalary"), // 지역별 급여
    LOCATION_POSTING_COUNT("locationPostingCount"), // 지역별 채용공고 수
    LOCATION_GROWTH("locationGrowth"), // 지역별 채용공고 증가율

    // 회사규모 관련
    COMPANY_SIZE_SKILL_DEMAND("companySizeSkillDemand"), // 회사규모별 기술 선호도
    COMPANY_SIZE_SALARY("companySizeSalary"), // 회사규모별 급여
    COMPANY_SIZE_BENEFIT("companySizeBenefit"), // 회사규모별 복리후생 제공 비율
    COMPANY_SIZE_POSTING_COUNT("companySizePostingCount"), // 회사규모별 채용공고 수
    COMPANY_SIZE_EDUCATION("companySizeEducation"), // 회사규모별 학력 요구 비율

    // 회사 순위
    COMPANY_HIRING_VOLUME("companyHiringVolume"), // 채용 규모 TOP 기업
    COMPANY_SALARY("companySalary"), // 기업별 평균 급여
    COMPANY_GROWTH("companyGrowth"), // 기업별 성장률
    COMPANY_RETENTION_RATE("companyRetentionRate"), // 기업별 이직률
    COMPANY_BENEFIT_COUNT("benefitCount"), // 복리후생 수

    // 복리후생 관련
    BENEFIT_POSTING_COUNT("benefitPostingCount"),

    // 학력별 평균 임금
    EDUCATION_SALARY("educationSalary"), // 학력별 평균 급여
    ;

    companion object {
        fun fromField(fieldName: String) =
            values().find { it.fieldName == fieldName }
                ?: throw IllegalArgumentException("Invalid field name: $fieldName")
    }
}

fun Class<*>.toRankingType(): RankingType =
    when (simpleName) {
        "BenefitPostingCountRankingsDocument" -> RankingType.BENEFIT_POSTING_COUNT
        "CompanyGrowthRankingsDocument" -> RankingType.COMPANY_GROWTH
        "CompanyHiringVolumeRankingsDocument" -> RankingType.COMPANY_HIRING_VOLUME
        "CompanyRetentionRateRankingsDocument" -> RankingType.COMPANY_RETENTION_RATE
        "CompanySalaryRankingsDocument" -> RankingType.COMPANY_SALARY
        "CompanySizeBenefitRankingsDocument" -> RankingType.COMPANY_SIZE_BENEFIT
        "CompanySizeEducationRankingsDocument" -> RankingType.COMPANY_SIZE_EDUCATION
        "CompanySizeSalaryRankingsDocument" -> RankingType.COMPANY_SIZE_SALARY
        "CompanySizeSkillRankingsDocument" -> RankingType.COMPANY_SIZE_SKILL_DEMAND
        "EducationSalaryRankingsDocument" -> RankingType.EDUCATION_SALARY
        "IndustryGrowthRankingsDocument" -> RankingType.INDUSTRY_GROWTH
        "IndustrySalaryRankingsDocument" -> RankingType.INDUSTRY_SALARY
        "IndustrySkillRankingsDocument" -> RankingType.INDUSTRY_SKILL
        "JobCategoryGrowthRankingsDocument" -> RankingType.JOB_CATEGORY_GROWTH
        "JobCategoryPostingCountRankingsDocument" -> RankingType.JOB_CATEGORY_POSTING_COUNT
        "JobCategorySalaryRankingsDocument" -> RankingType.JOB_CATEGORY_SALARY
        "JobCategorySkillRankingsDocument" -> RankingType.JOB_CATEGORY_SKILL
        "LocationPostingCountRankingsDocument" -> RankingType.LOCATION_POSTING_COUNT
        "LocationSalaryRankingsDocument" -> RankingType.LOCATION_SALARY
        "SkillGrowthRankingsDocument" -> RankingType.SKILL_GROWTH
        "SkillPostingCountRankingsDocument" -> RankingType.SKILL_POSTING_COUNT
        "SkillSalaryRankingsDocument" -> RankingType.SKILL_SALARY
        else -> throw IllegalArgumentException("알 수 없는 랭킹 문서 타입입니다: $simpleName")
    }

// 반대로 RankingType을 Document 클래스로 변환하는 함수도 추가
fun RankingType.toDocumentClassName(): String =
    when (this) {
        RankingType.BENEFIT_POSTING_COUNT -> "BenefitPostingCountRankingsDocument"
        RankingType.COMPANY_GROWTH -> "CompanyGrowthRankingsDocument"
        RankingType.COMPANY_HIRING_VOLUME -> "CompanyHiringVolumeRankingsDocument"
        RankingType.COMPANY_RETENTION_RATE -> "CompanyRetentionRateRankingsDocument"
        RankingType.COMPANY_SALARY -> "CompanySalaryRankingsDocument"
        RankingType.COMPANY_SIZE_BENEFIT -> "CompanySizeBenefitRankingsDocument"
        RankingType.COMPANY_SIZE_EDUCATION -> "CompanySizeEducationRankingsDocument"
        RankingType.COMPANY_SIZE_SALARY -> "CompanySizeSalaryRankingsDocument"
        RankingType.COMPANY_SIZE_SKILL_DEMAND -> "CompanySizeSkillRankingsDocument"
        RankingType.EDUCATION_SALARY -> "EducationSalaryRankingsDocument"
        RankingType.INDUSTRY_GROWTH -> "IndustryGrowthRankingsDocument"
        RankingType.INDUSTRY_SALARY -> "IndustrySalaryRankingsDocument"
        RankingType.INDUSTRY_SKILL -> "IndustrySkillRankingsDocument"
        RankingType.JOB_CATEGORY_GROWTH -> "JobCategoryGrowthRankingsDocument"
        RankingType.JOB_CATEGORY_POSTING_COUNT -> "JobCategoryPostingCountRankingsDocument"
        RankingType.JOB_CATEGORY_SALARY -> "JobCategorySalaryRankingsDocument"
        RankingType.JOB_CATEGORY_SKILL -> "JobCategorySkillRankingsDocument"
        RankingType.LOCATION_POSTING_COUNT -> "LocationPostingCountRankingsDocument"
        RankingType.LOCATION_SALARY -> "LocationSalaryRankingsDocument"
        RankingType.SKILL_GROWTH -> "SkillGrowthRankingsDocument"
        RankingType.SKILL_POSTING_COUNT -> "SkillPostingCountRankingsDocument"
        RankingType.SKILL_SALARY -> "SkillSalaryRankingsDocument"
        else -> throw IllegalArgumentException("해당 랭킹 타입에 대응하는 문서 클래스가 없습니다: $this")
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
        -> StatsType.COMPANY

        RankingType.COMPANY_HIRING_VOLUME,
        RankingType.COMPANY_SALARY,
        RankingType.COMPANY_GROWTH,
        RankingType.COMPANY_RETENTION_RATE,
        RankingType.COMPANY_BENEFIT_COUNT,
        -> StatsType.COMPANY

        RankingType.BENEFIT_POSTING_COUNT -> StatsType.BENEFIT

        RankingType.EDUCATION_SALARY -> StatsType.EDUCATION
    }
