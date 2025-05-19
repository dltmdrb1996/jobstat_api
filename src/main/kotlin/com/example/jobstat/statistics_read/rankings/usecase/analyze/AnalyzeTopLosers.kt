package com.example.jobstat.statistics_read.rankings.usecase.analyze

import com.example.jobstat.core.core_mongo_base.model.ranking.RankingEntry
import com.example.jobstat.core.core_model.BaseDate
import com.example.jobstat.core.core_usecase.base.ValidUseCase
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
