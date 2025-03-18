package com.example.jobstat.statistics.rankings.usecase

import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics.rankings.model.RankingWithStats
import com.example.jobstat.statistics.rankings.model.rankingtype.JobCategoryRankingType
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics.stats.document.JobCategoryStatsDocument
import com.example.jobstat.statistics.stats.document.JobCategoryStatsDocument.*
import io.swagger.v3.oas.annotations.media.Schema
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

//    @Cacheable(
//        cacheNames = ["statsWithRanking"],
//        key = "#request.rankingType + ':' + #request.baseDate + ':' + #request.page",
//        unless = "#result == null",
//    )
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
                rankingType = request.rankingType.toDomain(),
                baseDate = request.baseDate,
                page = request.page,
            )

        return Response(
            rankingType = request.rankingType.toDomain(),
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
        @field:NotNull val rankingType: JobCategoryRankingType,
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

    @Schema(name = "GetJobCategoryRankingWithStatsResponse", description = "직무 카테고리 순위 조회 응답")
    data class Response(
        @Schema(description = "순위 타입", example = "JOB_CATEGORY_SALARY")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<JobCategoryStatsDocument>>,
    )
}
