package com.example.jobstat.statistics_read.rankings.usecase.analyze

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnalyzeRankRange(
    private val rankingAnalysisService: RankingAnalysisService,
    validator: Validator,
) : ValidUseCase<AnalyzeRankRange.Request, AnalyzeRankRange.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val page =
            rankingAnalysisService.findRankRange(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                startRank = request.startRank,
                endRank = request.endRank,
            )
        return Response(page)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        @field:Min(1) val startRank: Int,
        @field:Min(1) val endRank: Int,
    )

    data class Response(
        val rankings: List<RankingEntry>,
    )
}
