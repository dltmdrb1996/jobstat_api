package com.example.jobstat.rankings.usecase

import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.base.mongo.ranking.RankingType
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.rankings.service.RankingAnalysisService
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class AnalyzeConsistentRankings(
    private val rankingAnalysisService: RankingAnalysisService,
    validator: Validator,
) : ValidUseCase<AnalyzeConsistentRankings.Request, AnalyzeConsistentRankings.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val page =
            rankingAnalysisService.findConsistentRankings(
                rankingType = request.rankingType,
                months = request.months,
                maxRank = request.maxRank,
            )
        return Response(page)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:Min(1) val months: Int,
        @field:Min(1) val maxRank: Int,
    )

    data class Response(
        val rankings: List<RankingEntry>,
    )
}
