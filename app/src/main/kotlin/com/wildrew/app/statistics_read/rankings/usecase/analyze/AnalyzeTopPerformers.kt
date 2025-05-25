package com.wildrew.app.statistics_read.rankings.usecase.analyze

import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.app.statistics_read.rankings.service.RankingAnalysisService
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
