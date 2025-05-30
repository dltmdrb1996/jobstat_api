package com.wildrew.jobstat.statistics_read.rankings.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.UseCase
import com.wildrew.jobstat.statistics_read.rankings.model.RankingWithStats
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.CompanyRankingType
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.wildrew.jobstat.statistics_read.stats.document.CompanyStatsDocument
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetCompanyRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetCompanyRankingWithStats.Request, GetCompanyRankingWithStats.Response> {
    companion object {
        val COMPANY_RANKING_TYPES =
            setOf(
                // Company Size rankings
                RankingType.COMPANY_SIZE_SKILL_DEMAND,
                RankingType.COMPANY_SIZE_SALARY,
                RankingType.COMPANY_SIZE_BENEFIT,
                RankingType.COMPANY_SIZE_POSTING_COUNT,
                RankingType.COMPANY_SIZE_EDUCATION,
                // Company rankings
                RankingType.COMPANY_HIRING_VOLUME,
                RankingType.COMPANY_SALARY,
                RankingType.COMPANY_GROWTH,
                RankingType.COMPANY_RETENTION_RATE,
                RankingType.COMPANY_BENEFIT_COUNT,
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
    fun execute(request: Request): Response =
        with(request) {
            // 기업 관련 통계와 순위 조회
            rankingAnalysisService
                .findStatsWithRanking<CompanyStatsDocument>(
                    rankingType = rankingType.toDomain(),
                    baseDate = baseDate,
                    page = page,
                ).let { result ->
                    // 응답 객체 생성
                    Response(
                        rankingType = rankingType.toDomain(),
                        totalCount = result.totalCount,
                        hasNextPage = result.hasNextPage,
                        items = result.items,
                    )
                }
        }

    data class Request(
        @field:NotNull val rankingType: CompanyRankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    @Schema(name = "GetCompanyRankingWithStatsResponse", description = "기업 순위 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "COMPANY_SALARY")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<CompanyStatsDocument>>,
    )
}
