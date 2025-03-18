package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics.rankings.model.RankingWithStats
import com.example.jobstat.statistics.rankings.model.rankingtype.EducationRankingType
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics.stats.document.EducationStatsDocument
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetEducationRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetEducationRankingWithStats.Request, GetEducationRankingWithStats.Response> {
    companion object {
        val EDUCATION_RANKING_TYPES =
            setOf(
                RankingType.EDUCATION_SALARY,
            )
    }

//    @Cacheable(
//        cacheNames = ["statsWithRanking"],
//        key = "#request.rankingType + ':' + #request.baseDate + ':' + #request.page",
//        unless = "#result == null",
//    )
    override operator fun invoke(request: Request): Response {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
        return execute(request)
    }

    @Transactional
    fun execute(request: Request): Response {
        val result =
            rankingAnalysisService.findStatsWithRanking<EducationStatsDocument>(
                rankingType = request.rankingType.toDomain(),
                baseDate = request.baseDate,
                page = request.page,
            )

        return Response(
            rankingType = request.rankingType.toDomain(),
            totalCount = result.totalCount,
            hasNextPage = result.hasNextPage,
            items = result.items,
        )
    }

    data class Request(
        @field:NotNull val rankingType: EducationRankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    @Schema(name = "GetEducationRankingWithStatsResponse", description = "학력 순위 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "EDUCATION_SALARY")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<EducationStatsDocument>>,
    )
}
