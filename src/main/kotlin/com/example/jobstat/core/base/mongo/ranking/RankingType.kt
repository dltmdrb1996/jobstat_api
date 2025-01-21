package com.example.jobstat.core.base.mongo.ranking

enum class RankingType(
    val fieldName: String,
) {
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
