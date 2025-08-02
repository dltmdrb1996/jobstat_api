package com.wildrew.jobstat.statistics_read.stats.usecase

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.UseCase
import com.wildrew.jobstat.statistics_read.stats.document.SkillStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetSkillProfile(
    private val statsAnalysisService: StatsAnalysisService,
    private val validator: Validator,
) : UseCase<GetSkillProfile.Request, GetSkillProfile.Response> {
    override operator fun invoke(request: Request): Response {
        validateRequest(request)
        return execute(request)
    }

    private fun validateRequest(request: Request) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
    }

    private fun execute(request: Request): Response {
        val skillStatsDocument =
            statsAnalysisService.findStatsByEntityIdAndBaseDate<SkillStatsDocument>(
                statsType = StatsType.SKILL,
                baseDate = request.baseDate,
                entityId = request.skillId,
            ) ?: throw AppException.fromErrorCode(ErrorCode.RESOURCE_NOT_FOUND)

        return Response(
            skillId = skillStatsDocument.entityId,
            skillName = skillStatsDocument.name,
            baseDate = skillStatsDocument.baseDate,
            coreStats = skillStatsDocument.stats,
            industryDistribution = skillStatsDocument.industryDistribution,
            relatedJobCategories = skillStatsDocument.relatedJobCategories,
            companySizeDistribution = skillStatsDocument.companySizeDistribution,
            experienceLevels = skillStatsDocument.experienceLevels,
        )
    }

    data class Request(
        @field:NotNull val skillId: Long,
        @field:NotNull val baseDate: BaseDate,
    )

    @Schema(name = "GetSkillProfileResponse", description = "기술 스택 상세 프로필 응답")
    data class Response(
        val skillId: Long,
        val skillName: String,
        val baseDate: String,
        val coreStats: SkillStatsDocument.SkillStats,
        val industryDistribution: List<SkillStatsDocument.IndustryDistribution>,
        val relatedJobCategories: List<SkillStatsDocument.RelatedJobCategory>,
        val companySizeDistribution: List<SkillStatsDocument.CompanySizeDistribution>,
        val experienceLevels: List<SkillStatsDocument.SkillExperienceLevel>,
    )
}
