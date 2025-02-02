package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class AnalyzeTopLosers(
    private val rankingAnalysisService: RankingAnalysisService,
    validator: Validator,
) : ValidUseCase<AnalyzeTopLosers.Request, AnalyzeTopLosers.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val page =
            rankingAnalysisService.findTopLosers(
                rankingType = request.rankingType,
                startDate = request.startDate,
                endDate = request.endDate,
                limit = request.limit,
            )
        return Response(page)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val startDate: BaseDate,
        @field:NotNull val endDate: BaseDate,
        @field:Min(1) val limit: Int = 10,
    )

    data class Response(
        val rankings: List<RankingEntry>,
    )
}
