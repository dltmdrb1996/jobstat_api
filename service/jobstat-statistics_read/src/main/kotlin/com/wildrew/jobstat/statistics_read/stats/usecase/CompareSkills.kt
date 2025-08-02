package com.wildrew.jobstat.statistics_read.stats.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.UseCase
import com.wildrew.jobstat.statistics_read.stats.document.SkillStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class CompareSkills(
    private val statsAnalysisService: StatsAnalysisService,
    private val validator: Validator,
) : UseCase<CompareSkills.Request, CompareSkills.Response> {
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
        val skillStatsMap =
            statsAnalysisService.findStatsByEntityIdsAndBaseDate<SkillStatsDocument>(
                statsType = StatsType.SKILL,
                baseDate = request.baseDate,
                entityIds = request.skillIds,
            )

        val comparisonItems =
            request.skillIds.mapNotNull { id ->
                skillStatsMap[id]?.let {
                    ComparisonItem(
                        skillId = it.entityId,
                        skillName = it.name,
                        coreStats = it.stats,
                    )
                }
            }

        return Response(
            baseDate = request.baseDate.toString(),
            comparison = comparisonItems,
        )
    }

    data class Request(
        @field:NotEmpty val skillIds: List<Long>,
        @field:NotNull val baseDate: BaseDate,
    )

    @Schema(name = "CompareSkillsResponse", description = "기술 스택 비교 응답")
    data class Response(
        val baseDate: String,
        val comparison: List<ComparisonItem>,
    )

    data class ComparisonItem(
        val skillId: Long,
        val skillName: String,
        val coreStats: SkillStatsDocument.SkillStats,
    )
}
