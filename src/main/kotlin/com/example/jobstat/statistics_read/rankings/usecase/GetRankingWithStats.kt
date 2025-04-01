package com.example.jobstat.statistics_read.rankings.usecase

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.UseCase
import com.example.jobstat.statistics_read.rankings.model.RankingWithStats
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.model.rankingtype.toStatsType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics_read.stats.document.*
import com.example.jobstat.statistics_read.stats.registry.StatsType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetRankingWithStats(
    private val rankingAnalysisService: RankingAnalysisService,
    private val validator: Validator,
) : UseCase<GetRankingWithStats.Request, GetRankingWithStats.Response<*>> {
//    @Cacheable(
//        cacheNames = ["statsWithRanking"],
//        key = "#request.rankingType + ':' + #request.baseDate + ':' + #request.page",
//        unless = "#result == null",
//    )
    override operator fun invoke(request: Request): Response<*> {
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
    fun execute(request: Request): Response<*> {
        // 통계 타입 결정
        val statsType = request.rankingType.toStatsType()
        
        // 통계 타입에 따라 적절한 통계 문서 타입으로 처리
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

    /**
     * 특정 통계 문서 타입에 대한 순위 및 통계 정보 조회
     */
    private inline fun <reified T : BaseStatsDocument> executeWithType(request: Request): Response<T> = 
        with(request) {
            // 특정 타입에 맞는 통계와 순위 정보 조회
            rankingAnalysisService.findStatsWithRanking<T>(
                rankingType = rankingType,
                baseDate = baseDate,
                page = page,
            ).let { result ->
                // 응답 생성
                Response(
                    statType = rankingType.toStatsType(),
                    rankingType = rankingType,
                    totalCount = result.totalCount,
                    hasNextPage = result.hasNextPage,
                    items = result.items,
                )
            }
        }

    data class Request(
        @field:NotNull val rankingType: RankingType,
        @field:NotNull val baseDate: BaseDate,
        val page: Int? = null,
    )

    @Schema(name = "GetRankingWithStatsResponse", description = "순위 및 통계 조회 응답")
    data class Response<T : BaseStatsDocument>(
        @Schema(description = "통계 타입", example = "SKILL")
        val statType: StatsType,
        @Schema(description = "순위 타입", example = "SKILL_SALARY")
        val rankingType: RankingType,
        @Schema(description = "총 데이터 수", example = "100")
        val totalCount: Int,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        val hasNextPage: Boolean,
        @Schema(description = "순위 및 통계 데이터 목록")
        val items: List<RankingWithStats<T>>,
    )
}
