package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.model.RankingWithStats
import com.example.jobstat.statistics.rankings.model.toStatsType
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics.stats.model.*
import com.example.jobstat.statistics.stats.registry.StatsType
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    validator: Validator,
) : ValidUseCase<GetRankingWithStats.Request, GetRankingWithStats.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> {
        val statsType: StatsType = request.rankingType.toStatsType()

        return when (statsType) {
            StatsType.BENEFIT -> executeWithType<BenefitStatsDocument>(request)
            StatsType.CERTIFICATION -> executeWithType<CertificationStatsDocument>(request)
            StatsType.COMPANY -> executeWithType<CompanyStatsDocument>(request)
            StatsType.CONTRACT_TYPE -> executeWithType<ContractTypeStatsDocument>(request)
            StatsType.EDUCATION -> executeWithType<EducationStatsDocument>(request)
            StatsType.EXPERIENCE -> executeWithType<ExperienceStatsDocument>(request)
            StatsType.INDUSTRY -> executeWithType<IndustryStatsDocument>(request)
            StatsType.JOB_CATEGORY -> executeWithType<JobCategoryStatsDocument>(request)
            StatsType.LOCATION -> executeWithType<LocationStatsDocument>(request)
            StatsType.REMOTE_WORK -> executeWithType<RemoteWorkStatsDocument>(request)
            StatsType.SKILL -> executeWithType<SkillStatsDocument>(request)
        }
    }

    private inline fun <reified T : BaseStatsDocument> executeWithType(request: Request): Response<T> {
        val rankings =
            rankingAnalysisService.findStatsWithRanking<T>(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                page = request.page,
            )
        return Response(rankings)
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    data class Response<T : BaseStatsDocument>(
        val rankings: List<RankingWithStats<T>>,
    )
}
