package com.example.jobstat.rankings

import ApiResponse
import com.example.jobstat.core.base.mongo.ranking.RankingType
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.rankings.usecase.GetRankingPage
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/rankings")
class RankingController(
    private val getRankingPage: GetRankingPage,
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
                lastRank = page,
            )
        return ApiResponse.ok(getRankingPage(req))
    }
}
