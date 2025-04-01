package com.example.jobstat.statistics_read.rankings.usecase

import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics_read.rankings.model.RankingWithStats
import com.example.jobstat.statistics_read.rankings.model.rankingtype.BenefitRankingType
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics_read.stats.document.BenefitStatsDocument
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetBenefitRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetBenefitRankingWithStats.Request, GetBenefitRankingWithStats.Response> {
    companion object {
        val BENEFIT_RANKING_TYPES =
            setOf(
                RankingType.BENEFIT_POSTING_COUNT,
            )
    }

//    @Cacheable(
//        cacheNames = ["statsWithRanking"],
//        key = "#request.rankingType + ':' + #request.baseDate + ':' + #request.page",
//        unless = "#result == null",
//    )
    override operator fun invoke(request: Request): Response {
        // 요청 유효성 검증
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
    fun execute(request: Request): Response = with(request) {
        // 통계 데이터와 함께 순위 조회
        rankingAnalysisService.findStatsWithRanking<BenefitStatsDocument>(
            rankingType = rankingType.toDomain(),
            baseDate = baseDate,
            page = page,
        ).let { result ->
            // 응답 생성
            Response(
                rankingType = rankingType.toDomain(),
                totalCount = result.totalCount,
                hasNextPage = result.hasNextPage,
                items = result.items,
            )
        }
    }

    data class Request(
        @field:NotNull val rankingType: BenefitRankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    @Schema(name = "GetBenefitRankingWithStatsResponse", description = "복리후생 순위 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "BENEFIT_POSTING_COUNT")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<BenefitStatsDocument>>,
    )
}
