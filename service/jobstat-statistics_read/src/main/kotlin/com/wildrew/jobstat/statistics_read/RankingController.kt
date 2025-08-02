package com.wildrew.jobstat.statistics_read

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import com.wildrew.jobstat.core.core_web_util.RestConstants
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.*
import com.wildrew.jobstat.statistics_read.rankings.usecase.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/rankings")
@Tag(name = "순위", description = "각종 통계 순위 조회 관련 API")
class RankingController(
    private val getSkillRankingWithStats: GetSkillRankingWithStats,
    private val getJobCategoryRankingWithStats: GetJobCategoryRankingWithStats,
    private val getIndustryRankingWithStats: GetIndustryRankingWithStats,
    private val getLocationRankingWithStats: GetLocationRankingWithStats,
    private val getBenefitRankingWithStats: GetBenefitRankingWithStats,
    private val getCompanySizeRankingWithStats: GetCompanySizeRankingWithStats,
    private val getExperienceRankingWithStats: GetExperienceRankingWithStats,
    private val getContractTypeRankingWithStats: GetContractTypeRankingWithStats,
    private val getRemoteWorkTypeRankingWithStats: GetRemoteWorkTypeRankingWithStats,
    private val getCompanyRankingWithStats: GetCompanyRankingWithStats,
    private val getCertificationRankingWithStats: GetCertificationRankingWithStats,
    private val getEducationRankingWithStats: GetEducationRankingWithStats,
) {
    @Public
    @GetMapping("/skills/{rankingType}/{baseDate}/stats")
    fun getSkillRankings(
        @PathVariable rankingType: SkillRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetSkillRankingWithStats.Response>> {
        val req =
            GetSkillRankingWithStats.Request(
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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetJobCategoryRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getJobCategoryRankings(
        @PathVariable rankingType: JobCategoryRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetJobCategoryRankingWithStats.Response>> {
        val req =
            GetJobCategoryRankingWithStats.Request(
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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetIndustryRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getIndustryRankings(
        @PathVariable rankingType: IndustryRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetIndustryRankingWithStats.Response>> {
        val req =
            GetIndustryRankingWithStats.Request(
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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetLocationRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getLocationRankings(
        @PathVariable rankingType: LocationRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetLocationRankingWithStats.Response>> {
        val req =
            GetLocationRankingWithStats.Request(
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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetBenefitRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getBenefitRankings(
        @PathVariable rankingType: BenefitRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetBenefitRankingWithStats.Response>> {
        val req =
            GetBenefitRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getBenefitRankingWithStats(req))
    }

    @Public
    @GetMapping("/company-size/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "회사 규모 순위 조회",
        description = "회사 규모 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetCompanySizeRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getCompanySizeRankings(
        @PathVariable rankingType: CompanySizeRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetCompanySizeRankingWithStats.Response>> {
        val req =
            GetCompanySizeRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getCompanySizeRankingWithStats(req))
    }

    @Public
    @GetMapping("/experience/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "경력 순위 조회",
        description = "경력 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetExperienceRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getExperienceRankings(
        @PathVariable rankingType: ExperienceRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetExperienceRankingWithStats.Response>> {
        val req =
            GetExperienceRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getExperienceRankingWithStats(req))
    }

    @Public
    @GetMapping("/contract-type/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "계약 형태 순위 조회",
        description = "계약 형태 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetContractTypeRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getContractTypeRankings(
        @PathVariable rankingType: ContractTypeRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetContractTypeRankingWithStats.Response>> {
        val req =
            GetContractTypeRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getContractTypeRankingWithStats(req))
    }

    @Public
    @GetMapping("/remote-work-type/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "원격 근무 형태 순위 조회",
        description = "원격 근무 형태 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetRemoteWorkTypeRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getRemoteWorkTypeRankings(
        @PathVariable rankingType: RemoteWorkTypeRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetRemoteWorkTypeRankingWithStats.Response>> {
        val req =
            GetRemoteWorkTypeRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getRemoteWorkTypeRankingWithStats(req))
    }

    @Public
    @GetMapping("/company/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "회사 순위 조회",
        description = "회사 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetCompanyRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getCompanyRankings(
        @PathVariable rankingType: CompanyRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetCompanyRankingWithStats.Response>> {
        val req =
            GetCompanyRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getCompanyRankingWithStats(req))
    }

    @Public
    @GetMapping("/certification/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "자격증 순위 조회",
        description = "자격증 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetCertificationRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getCertificationRankings(
        @PathVariable rankingType: CertificationRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetCertificationRankingWithStats.Response>> {
        val req =
            GetCertificationRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getCertificationRankingWithStats(req))
    }

    @Public
    @GetMapping("/education/{rankingType}/{baseDate}/stats")
    @Operation(
        summary = "학력 순위 조회",
        description = "학력 관련 순위와 통계를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetEducationRankingWithStats.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getEducationRankings(
        @PathVariable rankingType: EducationRankingType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetEducationRankingWithStats.Response>> {
        val req =
            GetEducationRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getEducationRankingWithStats(req))
    }
}
