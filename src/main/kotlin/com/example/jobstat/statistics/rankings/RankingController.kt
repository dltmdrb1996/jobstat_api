package com.example.jobstat.statistics.rankings

import ApiResponse
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.rankings.usecase.GetRankingPage
import com.example.jobstat.statistics.rankings.usecase.GetRankingWithStats
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/rankings")
class RankingController(
    private val getRankingPage: GetRankingPage,
    private val getRankingWithStats: GetRankingWithStats,
) {
    @Public
    @GetMapping("/{rankingType}/{baseDate}")
    fun getRankings(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetRankingPage.Response>> {
        val req =
            GetRankingPage.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getRankingPage(req))
    }

    @Public
    @GetMapping("/{rankingType}/{baseDate}/stats")
    fun getRankingsWithStats(
        @PathVariable rankingType: RankingType,
        @PathVariable baseDate: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetRankingWithStats.Response<*>>> {
        val req =
            GetRankingWithStats.Request(
                rankingType = rankingType,
                baseDate = BaseDate(baseDate),
                page = page,
            )
        return ApiResponse.ok(getRankingWithStats(req))
    }
}
