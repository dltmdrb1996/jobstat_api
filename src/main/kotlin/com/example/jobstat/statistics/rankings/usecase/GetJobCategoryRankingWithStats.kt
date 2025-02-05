package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.model.RankingWithStats
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics.stats.document.JobCategoryStatsDocument
import com.example.jobstat.statistics.stats.document.JobCategoryStatsDocument.*
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GetJobCategoryRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetJobCategoryRankingWithStats.Request, GetJobCategoryRankingWithStats.Response> {
    companion object {
        val JOB_CATEGORY_RANKING_TYPES =
            setOf(
                RankingType.JOB_CATEGORY_SKILL,
                RankingType.JOB_CATEGORY_APPLICATION_RATE,
                RankingType.JOB_CATEGORY_GROWTH,
                RankingType.JOB_CATEGORY_SALARY,
                RankingType.JOB_CATEGORY_POSTING_COUNT,
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
            rankingAnalysisService.findStatsWithRanking<JobCategoryStatsDocument>(
                rankingType = request.rankingType,
                baseDate = request.baseDate,
                page = request.page,
            )

        return Response(
            rankingType = request.rankingType,
            totalCount = result.totalCount,
            hasNextPage = result.hasNextPage,
            items =
                result.items.map {
                    RankingWithStats(
                        ranking = it.ranking,
                        stat = it.stat,
                    )
                },
        )
    }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    data class JobCategoryRankingStat(
        val stat: JobCategoryStats,
        val educationDistribution: List<EducationDistribution>,
        val salaryRangeDistribution: List<SalaryRangeDistribution>,
        val companySizeDistribution: List<CompanySizeDistribution>,
        val locationDistribution: List<LocationDistribution>,
        val contractTypeDistribution: List<ContractTypeDistribution>,
    )

    data class Response(
        val rankingType: RankingType,
        val totalCount: Int,
        val hasNextPage: Boolean,
        val items: List<RankingWithStats<JobCategoryStatsDocument>>,
    )
}
