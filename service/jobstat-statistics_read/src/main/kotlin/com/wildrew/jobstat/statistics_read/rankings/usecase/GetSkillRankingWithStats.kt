package com.wildrew.jobstat.statistics_read.rankings.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.UseCase
import com.wildrew.jobstat.statistics_read.rankings.model.RankingWithStats
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.SkillRankingType
import com.wildrew.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.wildrew.jobstat.statistics_read.stats.document.SkillStatsDocument
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetSkillRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetSkillRankingWithStats.Request, GetSkillRankingWithStats.Response> {
    companion object {
        val SKILL_RANKING_TYPES =
            setOf(
                RankingType.SKILL_SALARY,
                RankingType.SKILL_POSTING_COUNT,
                RankingType.SKILL_GROWTH,
                RankingType.SKILL_COMPETITION_RATE,
            )
    }

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

    @Transactional
    fun execute(request: Request): Response =
        with(request) {
            rankingAnalysisService
                .findStatsWithRanking<SkillStatsDocument>(
                    rankingType = rankingType.toDomain(),
                    baseDate = baseDate,
                    cursor = cursor,
                    limit = limit,
                ).let { result ->
                    Response(
                        rankingType = rankingType.toDomain(),
                        totalCount = result.totalCount,
                        hasNextPage = result.hasNextPage,
                        nextCursor = result.nextCursor,
                        items = result.items,
                    )
                }
        }

    data class Request(
        @field:NotNull val rankingType: SkillRankingType,
        @field:NotNull val baseDate: BaseDate,
        val cursor: Int? = null,
        @field:Max(100) val limit: Int = 20,
    )

    @Schema(name = "GetSkillRankingWithStatsResponse", description = "기술 순위 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "SKILL_SALARY")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "다음 페이지를 위한 커서 값", example = "20")
        val nextCursor: Int?,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<SkillStatsDocument>>,
    )
}
