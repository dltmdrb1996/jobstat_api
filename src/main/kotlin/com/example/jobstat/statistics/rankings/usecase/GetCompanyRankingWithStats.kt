package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.model.RankingWithStats
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics.stats.document.CompanyStatsDocument
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetCompanyRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetCompanyRankingWithStats.Request, GetCompanyRankingWithStats.Response> {
    companion object {
        val COMPANY_RANKING_TYPES =
            setOf(
                // Company Size rankings
                RankingType.COMPANY_SIZE_SKILL_DEMAND,
                RankingType.COMPANY_SIZE_SALARY,
                RankingType.COMPANY_SIZE_BENEFIT,
                RankingType.COMPANY_SIZE_POSTING_COUNT,
                RankingType.COMPANY_SIZE_EDUCATION,
                // Company rankings
                RankingType.COMPANY_HIRING_VOLUME,
                RankingType.COMPANY_SALARY,
                RankingType.COMPANY_GROWTH,
                RankingType.COMPANY_RETENTION_RATE,
                RankingType.COMPANY_BENEFIT_COUNT,
            )
    }

    @Cacheable(
        cacheNames = ["statsWithRanking"],
        key = "#request.rankingType + ':' + #request.baseDate + ':' + #request.page",
        unless = "#result == null",
    )
    override operator fun invoke(request: Request): Response {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
        return execute(request)
    }

    @Transactional
    fun execute(request: Request): Response {
        val result =
            rankingAnalysisService.findStatsWithRanking<CompanyStatsDocument>(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                page = request.page,
            )

        return Response(
            rankingType = request.rankingType,
            totalCount = result.totalCount,
            hasNextPage = result.hasNextPage,
            items = result.items,
        )
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    data class Response(
        val rankingType: RankingType,
        val totalCount: Int,
        val hasNextPage: Boolean,
        val items: List<RankingWithStats<CompanyStatsDocument>>,
    )
}
