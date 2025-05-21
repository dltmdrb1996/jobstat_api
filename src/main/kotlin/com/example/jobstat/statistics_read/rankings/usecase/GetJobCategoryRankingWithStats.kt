package com.example.jobstat.statistics_read.rankings.usecase

import com.example.jobstat.statistics_read.core.core_model.BaseDate
import com.example.jobstat.core.core_usecase.base.UseCase
import com.example.jobstat.statistics_read.rankings.model.RankingWithStats
import com.example.jobstat.statistics_read.rankings.model.rankingtype.JobCategoryRankingType
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics_read.stats.document.JobCategoryStatsDocument
import com.example.jobstat.statistics_read.stats.document.JobCategoryStatsDocument.*
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        // 요청 유효성 검증
        validateRequest(request)
        return execute(request)
    }

    private fun validateRequest(request: Request) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
    }

    @Transactional
    fun execute(request: Request): Response =
        with(request) {
            // 직무 카테고리 관련 통계와 순위 조회
            rankingAnalysisService
                .findStatsWithRanking<JobCategoryStatsDocument>(
                    rankingType = rankingType.toDomain(),
                    baseDate = baseDate,
                    page = page,
                ).let { result ->
                    // 응답 객체 생성
                    Response(
                        rankingType = rankingType.toDomain(),
                        totalCount = result.totalCount,
                        hasNextPage = result.hasNextPage,
                        items =
                            result.items.map { it ->
                                RankingWithStats(
                                    ranking = it.ranking,
                                    stat = it.stat,
                                )
                            },
                    )
                }
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
