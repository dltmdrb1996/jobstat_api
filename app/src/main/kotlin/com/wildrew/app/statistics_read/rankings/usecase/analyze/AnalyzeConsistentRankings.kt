package com.wildrew.app.statistics_read.rankings.usecase.analyze

import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.app.statistics_read.rankings.service.RankingAnalysisService
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
