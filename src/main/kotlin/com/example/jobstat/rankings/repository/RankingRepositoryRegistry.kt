package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument
import com.example.jobstat.core.base.mongo.ranking.RankingType
import com.example.jobstat.core.base.repository.BaseRankingRepository
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation

@Component
class RankingRepositoryRegistry(
    repositories: List<BaseRankingRepository<*, *, *>>,
) {
    private val repositoryMap: Map<RankingType, BaseRankingRepository<*, *, *>> = initializeRepositoryMap(repositories)

    private fun initializeRepositoryMap(repositories: List<BaseRankingRepository<*, *, *>>): Map<RankingType, BaseRankingRepository<*, *, *>> =
        repositories.associateBy { repo ->
            // Try to get RankingType from annotation first
            repo.javaClass.kotlin
                .findAnnotation<RankingRepositoryType>()
                ?.type
                ?: getRankingTypeFromClassName(repo.javaClass.simpleName)
                ?: throw IllegalArgumentException("Could not determine RankingType for repository: ${repo.javaClass}")
        }

    private fun getRankingTypeFromClassName(className: String): RankingType? =
        when {
            className.contains("BenefitPostingCount") -> RankingType.BENEFIT_POSTING_COUNT
            className.contains("CompanyGrowth") -> RankingType.COMPANY_GROWTH
            className.contains("CompanyHiringVolume") -> RankingType.COMPANY_HIRING_VOLUME
            className.contains("CompanyRetentionRate") -> RankingType.COMPANY_RETENTION_RATE
            className.contains("CompanySalary") -> RankingType.COMPANY_SALARY
            className.contains("CompanySizeBenefit") -> RankingType.COMPANY_SIZE_BENEFIT
            className.contains("CompanySizeEducation") -> RankingType.COMPANY_SIZE_EDUCATION
            className.contains("CompanySizeSalary") -> RankingType.COMPANY_SIZE_SALARY
            className.contains("CompanySizeSkill") -> RankingType.COMPANY_SIZE_SKILL_DEMAND
            className.contains("EducationSalary") -> RankingType.EDUCATION_SALARY
            className.contains("IndustryGrowth") -> RankingType.INDUSTRY_GROWTH
            className.contains("IndustrySalary") -> RankingType.INDUSTRY_SALARY
            className.contains("IndustrySkill") -> RankingType.INDUSTRY_SKILL
            className.contains("JobCategoryGrowth") -> RankingType.JOB_CATEGORY_GROWTH
            className.contains("JobCategoryPostingCount") -> RankingType.JOB_CATEGORY_POSTING_COUNT
            className.contains("JobCategorySalary") -> RankingType.JOB_CATEGORY_SALARY
            className.contains("JobCategorySkill") -> RankingType.JOB_CATEGORY_SKILL
            className.contains("LocationPostingCount") -> RankingType.LOCATION_POSTING_COUNT
            className.contains("LocationSalary") -> RankingType.LOCATION_SALARY
            className.contains("SkillGrowth") -> RankingType.SKILL_GROWTH
            className.contains("SkillPostingCount") -> RankingType.SKILL_POSTING_COUNT
            className.contains("SkillSalary") -> RankingType.SKILL_SALARY
            else -> null
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : BaseRankingDocument<*>> getRepository(rankingType: RankingType): BaseRankingRepository<T, *, *> =
        repositoryMap[rankingType] as? BaseRankingRepository<T, *, *>
            ?: throw IllegalArgumentException("Repository not found for type: $rankingType")

    fun hasRepository(rankingType: RankingType): Boolean = rankingType in repositoryMap
}
