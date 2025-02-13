package com.example.jobstat.statistics

import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.wrapper.ApiResponse
import com.example.jobstat.core.wrapper.ApiResponseWithType
import com.example.jobstat.statistics.rankings.model.rankingtype.BenefitRankingType
import com.example.jobstat.statistics.rankings.model.rankingtype.IndustryRankingType
import com.example.jobstat.statistics.rankings.model.rankingtype.JobCategoryRankingType
import com.example.jobstat.statistics.rankings.model.rankingtype.LocationRankingType
import com.example.jobstat.statistics.rankings.model.rankingtype.SkillRankingType
import com.example.jobstat.statistics.rankings.usecase.GetBenefitRankingWithStats
import com.example.jobstat.statistics.rankings.usecase.GetIndustryRankingWithStats
import com.example.jobstat.statistics.rankings.usecase.GetJobCategoryRankingWithStats
import com.example.jobstat.statistics.rankings.usecase.GetLocationRankingWithStats
import com.example.jobstat.statistics.rankings.usecase.GetSkillRankingWithStats
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/rankings")
@Tag(name = "순위", description = "각종 통계 순위 조회 관련 API")
class RankingController(
    private val getSkillRankingWithStats: GetSkillRankingWithStats,
    private val getJobCategoryRankingWithStats: GetJobCategoryRankingWithStats,
    private val getIndustryRankingWithStats: GetIndustryRankingWithStats,
    private val getLocationRankingWithStats: GetLocationRankingWithStats,
    private val getBenefitRankingWithStats: GetBenefitRankingWithStats,
) {

    @Public
    @GetMapping("/skills/{rankingType}/{baseDate}/stats")
    fun getSkillRankings(
        @PathVariable rankingType: SkillRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501"
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetSkillRankingWithStats.Response>> {
        val req = GetSkillRankingWithStats.Request(
            rankingType = rankingType,
            baseDate = BaseDate(baseDate),
            page = page,
        )
        return ApiResponse.ok(getSkillRankingWithStats(req))
    }

    @Public
    @GetMapping("/category/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "직무 카테고리 순위 조회",
        description = "직무 카테고리 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GetJobCategoryRankingWithStats.Response::class)
                )]
            )
        ]
    )
    fun getJobCategoryRankings(
        @PathVariable rankingType: JobCategoryRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501"
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetJobCategoryRankingWithStats.Response>> {
        val req = GetJobCategoryRankingWithStats.Request(
            rankingType = rankingType,
            baseDate = BaseDate(baseDate),
            page = page,
        )
        return ApiResponse.ok(getJobCategoryRankingWithStats(req))
    }

    @Public
    @GetMapping("/industry/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "산업군 순위 조회",
        description = "산업군 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GetIndustryRankingWithStats.Response::class)
                )]
            )
        ]
    )
    fun getIndustryRankings(
        @PathVariable rankingType: IndustryRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501"
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetIndustryRankingWithStats.Response>> {
        val req = GetIndustryRankingWithStats.Request(
            rankingType = rankingType,
            baseDate = BaseDate(baseDate),
            page = page,
        )
        return ApiResponse.ok(getIndustryRankingWithStats(req))
    }

    @Public
    @GetMapping("/location/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "지역 순위 조회",
        description = "지역별 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GetLocationRankingWithStats.Response::class)
                )]
            )
        ]
    )
    fun getLocationRankings(
        @PathVariable rankingType: LocationRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501"
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetLocationRankingWithStats.Response>> {
        val req = GetLocationRankingWithStats.Request(
            rankingType = rankingType,
            baseDate = BaseDate(baseDate),
            page = page,
        )
        return ApiResponse.ok(getLocationRankingWithStats(req))
    }

    @Public
    @GetMapping("/benefit/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "복리후생 순위 조회",
        description = "복리후생 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GetBenefitRankingWithStats.Response::class)
                )]
            )
        ]
    )
    fun getBenefitRankings(
        @PathVariable rankingType: BenefitRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501"
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetBenefitRankingWithStats.Response>> {
        val req = GetBenefitRankingWithStats.Request(
            rankingType = rankingType,
            baseDate = BaseDate(baseDate),
            page = page,
        )
        return ApiResponse.ok(getBenefitRankingWithStats(req))
    }
}
