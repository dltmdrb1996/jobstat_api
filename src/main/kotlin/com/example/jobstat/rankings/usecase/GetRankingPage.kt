package com.example.jobstat.rankings.usecase

import com.example.jobstat.core.base.mongo.ranking.RankingType
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.rankings.model.RankingPage
import com.example.jobstat.rankings.service.RankingAnalysisService
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
    override fun execute(request: Request): Response {
        val page =
            rankingAnalysisService.findRankingPage(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                lastRank = request.lastRank,
            )
        return Response(page)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val lastRank: Int? = null,
    )

    data class Response(
        val page: RankingPage,
    )
}
