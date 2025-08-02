package com.wildrew.jobstat.statistics_read

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import com.wildrew.jobstat.core.core_web_util.RestConstants
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.*
import com.wildrew.jobstat.statistics_read.rankings.usecase.*
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/rankings")
@Tag(name = "순위", description = "각종 통계 순위 조회 관련 API")
class RankingController(
    // Heavy UseCases
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
    // Light UseCases
    private val getSkillRanking: GetSkillRanking,
    private val getJobCategoryRanking: GetJobCategoryRanking,
    private val getIndustryRanking: GetIndustryRanking,
    private val getLocationRanking: GetLocationRanking,
    private val getBenefitRanking: GetBenefitRanking,
    private val getCompanySizeRanking: GetCompanySizeRanking,
    private val getExperienceRanking: GetExperienceRanking,
    private val getContractTypeRanking: GetContractTypeRanking,
    private val getRemoteWorkTypeRanking: GetRemoteWorkTypeRanking,
    private val getCompanyRanking: GetCompanyRanking,
    private val getCertificationRanking: GetCertificationRanking,
    private val getEducationRanking: GetEducationRanking,
) {
    // --- Skill ---
    @Public
    @GetMapping("/skills/{rankingType}/{baseDate}/stats")
    fun getSkillRankingsWithStats(
        @PathVariable rankingType: SkillRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetSkillRankingWithStats.Response>> {
        val req = GetSkillRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getSkillRankingWithStats(req))
    }

    @Public
    @GetMapping("/skills/{rankingType}/{baseDate}")
    fun getSkillRankings(
        @PathVariable rankingType: SkillRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetSkillRanking.Response>> {
        val req = GetSkillRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getSkillRanking(req))
    }

    // --- Job Category ---
    @Public
    @GetMapping("/category/{rankingType}/{baseDate}/stats")
    fun getJobCategoryRankingsWithStats(
        @PathVariable rankingType: JobCategoryRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetJobCategoryRankingWithStats.Response>> {
        val req = GetJobCategoryRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getJobCategoryRankingWithStats(req))
    }

    @Public
    @GetMapping("/category/{rankingType}/{baseDate}")
    fun getJobCategoryRankings(
        @PathVariable rankingType: JobCategoryRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetJobCategoryRanking.Response>> {
        val req = GetJobCategoryRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getJobCategoryRanking(req))
    }

    // --- Industry ---
    @Public
    @GetMapping("/industry/{rankingType}/{baseDate}/stats")
    fun getIndustryRankingsWithStats(
        @PathVariable rankingType: IndustryRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetIndustryRankingWithStats.Response>> {
        val req = GetIndustryRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getIndustryRankingWithStats(req))
    }

    @Public
    @GetMapping("/industry/{rankingType}/{baseDate}")
    fun getIndustryRankings(
        @PathVariable rankingType: IndustryRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetIndustryRanking.Response>> {
        val req = GetIndustryRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getIndustryRanking(req))
    }

    // --- Location ---
    @Public
    @GetMapping("/location/{rankingType}/{baseDate}/stats")
    fun getLocationRankingsWithStats(
        @PathVariable rankingType: LocationRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetLocationRankingWithStats.Response>> {
        val req = GetLocationRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getLocationRankingWithStats(req))
    }

    @Public
    @GetMapping("/location/{rankingType}/{baseDate}")
    fun getLocationRankings(
        @PathVariable rankingType: LocationRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetLocationRanking.Response>> {
        val req = GetLocationRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getLocationRanking(req))
    }

    // --- Benefit ---
    @Public
    @GetMapping("/benefit/{rankingType}/{baseDate}/stats")
    fun getBenefitRankingsWithStats(
        @PathVariable rankingType: BenefitRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetBenefitRankingWithStats.Response>> {
        val req = GetBenefitRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getBenefitRankingWithStats(req))
    }

    @Public
    @GetMapping("/benefit/{rankingType}/{baseDate}")
    fun getBenefitRankings(
        @PathVariable rankingType: BenefitRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetBenefitRanking.Response>> {
        val req = GetBenefitRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getBenefitRanking(req))
    }

    // --- Company Size ---
    @Public
    @GetMapping("/company-size/{rankingType}/{baseDate}/stats")
    fun getCompanySizeRankingsWithStats(
        @PathVariable rankingType: CompanySizeRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCompanySizeRankingWithStats.Response>> {
        val req = GetCompanySizeRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getCompanySizeRankingWithStats(req))
    }

    @Public
    @GetMapping("/company-size/{rankingType}/{baseDate}")
    fun getCompanySizeRankings(
        @PathVariable rankingType: CompanySizeRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCompanySizeRanking.Response>> {
        val req = GetCompanySizeRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getCompanySizeRanking(req))
    }

    // --- Experience ---
    @Public
    @GetMapping("/experience/{rankingType}/{baseDate}/stats")
    fun getExperienceRankingsWithStats(
        @PathVariable rankingType: ExperienceRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetExperienceRankingWithStats.Response>> {
        val req = GetExperienceRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getExperienceRankingWithStats(req))
    }

    @Public
    @GetMapping("/experience/{rankingType}/{baseDate}")
    fun getExperienceRankings(
        @PathVariable rankingType: ExperienceRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetExperienceRanking.Response>> {
        val req = GetExperienceRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getExperienceRanking(req))
    }

    // --- Contract Type ---
    @Public
    @GetMapping("/contract-type/{rankingType}/{baseDate}/stats")
    fun getContractTypeRankingsWithStats(
        @PathVariable rankingType: ContractTypeRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetContractTypeRankingWithStats.Response>> {
        val req = GetContractTypeRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getContractTypeRankingWithStats(req))
    }

    @Public
    @GetMapping("/contract-type/{rankingType}/{baseDate}")
    fun getContractTypeRankings(
        @PathVariable rankingType: ContractTypeRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetContractTypeRanking.Response>> {
        val req = GetContractTypeRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getContractTypeRanking(req))
    }

    // --- Remote Work Type ---
    @Public
    @GetMapping("/remote-work-type/{rankingType}/{baseDate}/stats")
    fun getRemoteWorkTypeRankingsWithStats(
        @PathVariable rankingType: RemoteWorkTypeRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetRemoteWorkTypeRankingWithStats.Response>> {
        val req = GetRemoteWorkTypeRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getRemoteWorkTypeRankingWithStats(req))
    }

    @Public
    @GetMapping("/remote-work-type/{rankingType}/{baseDate}")
    fun getRemoteWorkTypeRankings(
        @PathVariable rankingType: RemoteWorkTypeRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetRemoteWorkTypeRanking.Response>> {
        val req = GetRemoteWorkTypeRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getRemoteWorkTypeRanking(req))
    }

    // --- Company ---
    @Public
    @GetMapping("/company/{rankingType}/{baseDate}/stats")
    fun getCompanyRankingsWithStats(
        @PathVariable rankingType: CompanyRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCompanyRankingWithStats.Response>> {
        val req = GetCompanyRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getCompanyRankingWithStats(req))
    }

    @Public
    @GetMapping("/company/{rankingType}/{baseDate}")
    fun getCompanyRankings(
        @PathVariable rankingType: CompanyRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCompanyRanking.Response>> {
        val req = GetCompanyRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getCompanyRanking(req))
    }

    // --- Certification ---
    @Public
    @GetMapping("/certification/{rankingType}/{baseDate}/stats")
    fun getCertificationRankingsWithStats(
        @PathVariable rankingType: CertificationRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCertificationRankingWithStats.Response>> {
        val req = GetCertificationRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getCertificationRankingWithStats(req))
    }

    @Public
    @GetMapping("/certification/{rankingType}/{baseDate}")
    fun getCertificationRankings(
        @PathVariable rankingType: CertificationRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCertificationRanking.Response>> {
        val req = GetCertificationRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getCertificationRanking(req))
    }

    // --- Education ---
    @Public
    @GetMapping("/education/{rankingType}/{baseDate}/stats")
    fun getEducationRankingsWithStats(
        @PathVariable rankingType: EducationRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetEducationRankingWithStats.Response>> {
        val req = GetEducationRankingWithStats.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getEducationRankingWithStats(req))
    }

    @Public
    @GetMapping("/education/{rankingType}/{baseDate}")
    fun getEducationRankings(
        @PathVariable rankingType: EducationRankingType,
        @Parameter(description = "기준 날짜 (YYYYMM)", schema = Schema(defaultValue = "202501", type = "string"), example = "202501") @PathVariable baseDate: String,
        @RequestParam(required = false) cursor: Int?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetEducationRanking.Response>> {
        val req = GetEducationRanking.Request(rankingType = rankingType, baseDate = BaseDate(baseDate), cursor = cursor, limit = limit)
        return ApiResponse.ok(getEducationRanking(req))
    }
}
