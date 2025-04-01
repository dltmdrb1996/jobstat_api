package com.example.jobstat.statistics_read.rankings.model.rankingtype

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "기술 순위 전용 타입")
enum class SkillRankingType(
    val domain: RankingType,
) {
    @Schema(description = "기술별 평균 급여")
    SKILL_SALARY(RankingType.SKILL_SALARY),

    @Schema(description = "기술별 채용공고 수")
    SKILL_POSTING_COUNT(RankingType.SKILL_POSTING_COUNT),

    @Schema(description = "기술별 성장률")
    SKILL_GROWTH(RankingType.SKILL_GROWTH),

//    @Schema(description = "기술별 경쟁률")
//    SKILL_COMPETITION_RATE(RankingType.SKILL_COMPETITION_RATE),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "직무 카테고리 순위 전용 타입")
enum class JobCategoryRankingType(
    val domain: RankingType,
) {
    //    @Schema(description = "직무별 기술 선호도")
//    JOB_CATEGORY_SKILL(RankingType.JOB_CATEGORY_SKILL),
//
//    @Schema(description = "직무 지원율")
//    JOB_CATEGORY_APPLICATION_RATE(RankingType.JOB_CATEGORY_APPLICATION_RATE),

    @Schema(description = "직무 공고 증가율")
    JOB_CATEGORY_GROWTH(RankingType.JOB_CATEGORY_GROWTH),

    @Schema(description = "직무 평균 급여")
    JOB_CATEGORY_SALARY(RankingType.JOB_CATEGORY_SALARY),

    @Schema(description = "직무 채용공고 수")
    JOB_CATEGORY_POSTING_COUNT(RankingType.JOB_CATEGORY_POSTING_COUNT),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "업종 순위 전용 타입")
enum class IndustryRankingType(
    val domain: RankingType,
) {
    //    @Schema(description = "업종별 기술 선호도")
//    INDUSTRY_SKILL(RankingType.INDUSTRY_SKILL),

    @Schema(description = "채용 성장 TOP 산업")
    INDUSTRY_GROWTH(RankingType.INDUSTRY_GROWTH),

    @Schema(description = "업종별 평균 급여")
    INDUSTRY_SALARY(RankingType.INDUSTRY_SALARY),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "자격증 순위 전용 타입")
enum class CertificationRankingType(
    val domain: RankingType,
) {
    @Schema(description = "자격증별 채용공고 수")
    CERTIFICATION_POSTING_COUNT(RankingType.CERTIFICATION_POSTING_COUNT),

    @Schema(description = "자격증별 급여")
    CERTIFICATION_SALARY(RankingType.CERTIFICATION_SALARY),

    @Schema(description = "자격증별 성장률")
    CERTIFICATION_GROWTH(RankingType.CERTIFICATION_GROWTH),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "지역 순위 전용 타입")
enum class LocationRankingType(
    val domain: RankingType,
) {
    @Schema(description = "지역별 급여")
    LOCATION_SALARY(RankingType.LOCATION_SALARY),

    @Schema(description = "지역별 채용공고 수")
    LOCATION_POSTING_COUNT(RankingType.LOCATION_POSTING_COUNT),

//    @Schema(description = "지역별 채용공고 증가율")
//    LOCATION_GROWTH(RankingType.LOCATION_GROWTH),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "회사 규모 순위 전용 타입")
enum class CompanySizeRankingType(
    val domain: RankingType,
) {
    @Schema(description = "회사규모별 기술 선호도")
    COMPANY_SIZE_SKILL_DEMAND(RankingType.COMPANY_SIZE_SKILL_DEMAND),

    @Schema(description = "회사규모별 급여")
    COMPANY_SIZE_SALARY(RankingType.COMPANY_SIZE_SALARY),

    @Schema(description = "회사규모별 복리후생 제공 비율")
    COMPANY_SIZE_BENEFIT(RankingType.COMPANY_SIZE_BENEFIT),

    @Schema(description = "회사규모별 채용공고 수")
    COMPANY_SIZE_POSTING_COUNT(RankingType.COMPANY_SIZE_POSTING_COUNT),

    @Schema(description = "회사규모별 학력 요구 비율")
    COMPANY_SIZE_EDUCATION(RankingType.COMPANY_SIZE_EDUCATION),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "회사 순위 전용 타입")
enum class CompanyRankingType(
    val domain: RankingType,
) {
    @Schema(description = "채용 규모 TOP 기업")
    COMPANY_HIRING_VOLUME(RankingType.COMPANY_HIRING_VOLUME),

    @Schema(description = "기업별 평균 급여")
    COMPANY_SALARY(RankingType.COMPANY_SALARY),

    @Schema(description = "기업별 성장률")
    COMPANY_GROWTH(RankingType.COMPANY_GROWTH),

    @Schema(description = "기업별 이직률")
    COMPANY_RETENTION_RATE(RankingType.COMPANY_RETENTION_RATE),

    @Schema(description = "복리후생 수")
    COMPANY_BENEFIT_COUNT(RankingType.COMPANY_BENEFIT_COUNT),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "복리후생 순위 전용 타입")
enum class BenefitRankingType(
    val domain: RankingType,
) {
    @Schema(description = "복리후생 관련 채용공고 수")
    BENEFIT_POSTING_COUNT(RankingType.BENEFIT_POSTING_COUNT),
    ;

    fun toDomain(): RankingType = domain
}

@Schema(description = "학력 순위 전용 타입")
enum class EducationRankingType(
    val domain: RankingType,
) {
    @Schema(description = "학력별 평균 급여")
    EDUCATION_SALARY(RankingType.EDUCATION_SALARY),
    ;

    fun toDomain(): RankingType = domain
}
