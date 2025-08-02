package com.wildrew.jobstat.statistics_read.rankings.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.UseCase
import com.wildrew.jobstat.statistics_read.rankings.model.PureRankingPage
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.CompanySizeRankingType
import com.wildrew.jobstat.statistics_read.rankings.service.RankingAnalysisService
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetCompanySizeRanking(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetCompanySizeRanking.Request, GetCompanySizeRanking.Response> {
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
                .findRankingOnly(
                    rankingType = rankingType.toDomain(),
                    baseDate = baseDate,
                    cursor = cursor,
                    limit = limit,
                ).let { result ->
                    Response(
                        rankingType = rankingType,
                        page = result,
                    )
                }
        }

    data class Request(
        @field:NotNull val rankingType: CompanySizeRankingType,
        @field:NotNull val baseDate: BaseDate,
        val cursor: Int? = null,
        @field:Max(100) val limit: Int = 20,
    )

    @Schema(name = "GetCompanySizeRankingResponse", description = "회사 규모 순위(경량) 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "COMPANY_SIZE_POSTING_COUNT")
        val rankingType: CompanySizeRankingType,
        @Schema(description = "순위 데이터 페이지")
        val page: PureRankingPage,
    )
}
