package com.wildrew.jobstat.statistics_read.rankings.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.UseCase
import com.wildrew.jobstat.statistics_read.rankings.model.RankingWithStats
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.EducationRankingType
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.wildrew.jobstat.statistics_read.stats.document.EducationStatsDocument
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetEducationRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetEducationRankingWithStats.Request, GetEducationRankingWithStats.Response> {
    companion object {
        val EDUCATION_RANKING_TYPES =
            setOf(
                RankingType.EDUCATION_POSTING_COUNT,
                RankingType.EDUCATION_SALARY,
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
                .findStatsWithRanking<EducationStatsDocument>(
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
        @field:NotNull val rankingType: EducationRankingType,
        @field:NotNull val baseDate: BaseDate,
        val cursor: Int? = null,
        @field:Max(100) val limit: Int = 20,
    )

    @Schema(name = "GetEducationRankingWithStatsResponse", description = "학력 순위 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "EDUCATION_SALARY")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "다음 페이지를 위한 커서 값", example = "20")
        val nextCursor: Int?,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<EducationStatsDocument>>,
    )
}
