package com.example.jobstat.statistics_read.rankings.usecase.analyze

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class AnalyzeTopPerformers(
    private val rankingAnalysisService: RankingAnalysisService,
    validator: Validator,
) : ValidUseCase<AnalyzeTopPerformers.Request, AnalyzeTopPerformers.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val page =
            rankingAnalysisService.findTopNRankings(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                limit = request.limit,
            )
        return Response(page)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        @field:Min(1) val limit: Int = 10,
    )

    data class Response(
        val rankings: List<RankingEntry>,
    )
}
