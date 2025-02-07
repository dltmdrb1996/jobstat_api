package com.example.jobstat.statistics.rankings

import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.wrapper.ApiResponse
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.usecase.*
import com.example.jobstat.statistics.rankings.usecase.GetBenefitRankingWithStats.Companion.BENEFIT_RANKING_TYPES
import com.example.jobstat.statistics.rankings.usecase.GetIndustryRankingWithStats.Companion.INDUSTRY_RANKING_TYPES
import com.example.jobstat.statistics.rankings.usecase.GetJobCategoryRankingWithStats.Companion.JOB_CATEGORY_RANKING_TYPES
import com.example.jobstat.statistics.rankings.usecase.GetLocationRankingWithStats.Companion.LOCATION_RANKING_TYPES
import com.example.jobstat.statistics.rankings.usecase.GetSkillRankingWithStats.Companion.SKILL_RANKING_TYPES
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/rankings")
class RankingController(
    private val getRankingPage: GetRankingPage,
    private val getRankingWithStats: GetRankingWithStats,
    private val getSkillRankingWithStats: GetSkillRankingWithStats,
    private val getJobCategoryRankingWithStats: GetJobCategoryRankingWithStats,
    private val getIndustryRankingWithStats: GetIndustryRankingWithStats,
    private val getLocationRankingWithStats: GetLocationRankingWithStats,
    private val getBenefitRankingWithStats: GetBenefitRankingWithStats,
) {
//    @Public
//    @GetMapping("/{rankingType}/{baseDate}")
//    fun getRankings(
//        @PathVariable rankingType: RankingType,
//        @PathVariable baseDate: String,
//        @RequestParam(required = false) page: Int?,
//    ): ResponseEntity<ApiResponse<GetRankingPage.Response>> {
//        val req =
//            GetRankingPage.Request(
//                rankingType = rankingType,
//                baseDate = BaseDate(baseDate),
//                page = page,
//            )
//        return ApiResponse.ok(getRankingPage(req))
//    }

    @Public
    @GetMapping("/skills/{rankingType}/{baseDate}/stats")
    fun getSkillRankings(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetSkillRankingWithStats.Response>> {
        require(rankingType in SKILL_RANKING_TYPES) {
            "잘못된 순위 유형입니다. $SKILL_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
        }
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
    fun getJobCategoryRankings(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetJobCategoryRankingWithStats.Response>> {
        require(rankingType in JOB_CATEGORY_RANKING_TYPES) {
            "잘못된 순위 유형입니다. $JOB_CATEGORY_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
        }
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
    fun getIndustryRankings(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetIndustryRankingWithStats.Response>> {
        require(rankingType in INDUSTRY_RANKING_TYPES) {
            "잘못된 순위 유형입니다. $INDUSTRY_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
        }
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
    fun getLocationRankings(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetLocationRankingWithStats.Response>> {
        require(rankingType in LOCATION_RANKING_TYPES) {
            "잘못된 순위 유형입니다. $LOCATION_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
        }
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
    fun getBenefitRankings(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetBenefitRankingWithStats.Response>> {
        require(rankingType in BENEFIT_RANKING_TYPES) {
            "잘못된 순위 유형입니다. $BENEFIT_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
        }
        val req =
            GetBenefitRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getBenefitRankingWithStats(req))
    }

//    @Public
//    @GetMapping("/company/{rankingType}/{baseDate}/stats")
//    fun getCompanyRankings(
//        @PathVariable rankingType: RankingType,
//        @PathVariable baseDate: String,
//        @RequestParam(required = false) page: Int?,
//    ): ResponseEntity<ApiResponse<GetCompanyRankingWithStats.Response>> {
//        require(rankingType in COMPANY_RANKING_TYPES) {
//            "잘못된 순위 유형입니다. $COMPANY_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
//        }
//        val req =
//            GetCompanyRankingWithStats.Request(
//                rankingType = rankingType,
//                baseDate = BaseDate(baseDate),
//                page = page,
//            )
//        return ApiResponse.ok(getCompanyRankingWithStats(req))
//    }

//    @Public
//    @GetMapping("/certification/{rankingType}/{baseDate}/stats")
//    fun getCertificationRankings(
//        @PathVariable rankingType: RankingType,
//        @PathVariable baseDate: String,
//        @RequestParam(required = false) page: Int?,
//    ): ResponseEntity<ApiResponse<GetCertificationRankingWithStats.Response>> {
//        require(rankingType in CERTIFICATION_RANKING_TYPES) {
//            "잘못된 순위 유형입니다. $CERTIFICATION_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
//        }
//        val req =
//            GetCertificationRankingWithStats.Request(
//                rankingType = rankingType,
//                baseDate = BaseDate(baseDate),
//                page = page,
//            )
//        return ApiResponse.ok(getCertificationRankingWithStats(req))
//    }

//    @Public
//    @GetMapping("/education/{rankingType}/{baseDate}/stats")
//    fun getEducationRankings(
//        @PathVariable rankingType: RankingType,
//        @PathVariable baseDate: String,
//        @RequestParam(required = false) page: Int?,
//    ): ResponseEntity<ApiResponse<GetEducationRankingWithStats.Response>> {
//        require(rankingType in EDUCATION_RANKING_TYPES) {
//            "잘못된 순위 유형입니다. $EDUCATION_RANKING_TYPES 중 하나여야 하지만 $rankingType 입니다"
//        }
//        val req =
//            GetEducationRankingWithStats.Request(
//                rankingType = rankingType,
//                baseDate = BaseDate(baseDate),
//                page = page,
//            )
//        return ApiResponse.ok(getEducationRankingWithStats(req))
//    }
}
