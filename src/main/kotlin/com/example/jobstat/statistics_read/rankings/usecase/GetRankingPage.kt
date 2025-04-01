package com.example.jobstat.statistics_read.rankings.usecase

import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics_read.rankings.model.RankingPage
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetRankingPage(
    private val rankingAnalysisService: RankingAnalysisService,
    validator: Validator,
) : ValidUseCase<GetRankingPage.Request, GetRankingPage.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response = with(request) {
        // 순위 페이지 조회 및 응답 생성
        rankingAnalysisService.findRankingPage(
            rankingType = rankingType,
            baseDate = baseDate,
            page = page,
        ).let(::Response)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    @Schema(name = "GetRankingPageResponse", description = "순위 페이지 조회 응답")
    data class Response(
        @Schema(description = "순위 페이지 정보")
        val page: RankingPage,
    )
}
