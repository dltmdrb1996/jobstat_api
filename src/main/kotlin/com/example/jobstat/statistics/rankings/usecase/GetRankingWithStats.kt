package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.model.RankingWithStats
import com.example.jobstat.statistics.rankings.model.toStatsType
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics.stats.document.*
import com.example.jobstat.statistics.stats.registry.StatsType
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetRankingWithStats.Request, GetRankingWithStats.Response<*>> {
    @Cacheable(
        cacheNames = ["statsWithRanking"],
        key = "#request.rankingType + ':' + #request.baseDate + ':' + #request.page",
        unless = "#result == null",
    )
    override operator fun invoke(request: Request): Response<*> {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
        return execute(request)
    }

    @Transactional
    fun execute(request: Request): Response<*> {
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
        val result =
            rankingAnalysisService.findStatsWithRanking<T>(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                page = request.page,
            )
        return Response(
            statType = request.rankingType.toStatsType(),
            rankingType = request.rankingType,
            result.totalCount,
            result.hasNextPage,
            result.items,
        )
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    data class Response<T : BaseStatsDocument>(
        val statType: StatsType,
        val rankingType: RankingType,
        val totalCount: Int,
        val hasNextPage: Boolean,
        val items: List<RankingWithStats<T>>,
    )
}
