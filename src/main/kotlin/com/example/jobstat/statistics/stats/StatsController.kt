package com.example.jobstat.statistics.stats

import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.wrapper.ApiResponse
import com.example.jobstat.statistics.stats.registry.StatsType
import com.example.jobstat.statistics.stats.usecase.GetLatestStats
import com.example.jobstat.statistics.stats.usecase.GetStatsByEntityId
import com.example.jobstat.statistics.stats.usecase.GetStatsByEntityIdAndBaseDate
import com.example.jobstat.statistics.stats.usecase.GetStatsByEntityIds
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/stats")
class StatsController(
    private val getStatsByEntityId: GetStatsByEntityId,
    private val getStatsByEntityIds: GetStatsByEntityIds,
    private val getLatestStats: GetLatestStats,
    private val getStatsByEntityIdAndBaseDate: GetStatsByEntityIdAndBaseDate,
) {
    @Public
    @GetMapping("/{statsType}/entity/{entityId}")
    fun getStatsByEntityId(
        @PathVariable statsType: StatsType,
        @PathVariable entityId: Long,
    ): ResponseEntity<ApiResponse<GetStatsByEntityId.Response<*>>> {
        val req =
            GetStatsByEntityId.Request(
                statsType = statsType,
                entityId = entityId,
            )
        return ApiResponse.ok(getStatsByEntityId(req))
    }

    @Public
    @PostMapping("/{statsType}/{baseDate}/entities")
    fun getStatsByEntityIds(
        @PathVariable statsType: StatsType,
        @PathVariable baseDate: String,
        @RequestBody entityIds: List<Long>,
    ): ResponseEntity<ApiResponse<GetStatsByEntityIds.Response<*>>> {
        val req =
            GetStatsByEntityIds.Request(
                statsType = statsType,
                baseDate = BaseDate(baseDate),
                entityIds = entityIds,
            )
        return ApiResponse.ok(getStatsByEntityIds(req))
    }

    @Public
    @GetMapping("/{statsType}/entity/{entityId}/latest")
    fun getLatestStats(
        @PathVariable statsType: StatsType,
        @PathVariable entityId: Long,
    ): ResponseEntity<ApiResponse<GetLatestStats.Response<*>>> {
        val req =
            GetLatestStats.Request(
                statsType = statsType,
                entityId = entityId,
            )
        return ApiResponse.ok(getLatestStats(req))
    }

    @Public
    @GetMapping("/{statsType}/{baseDate}/entity/{entityId}")
    fun getStatsByEntityIdAndBaseDate(
        @PathVariable statsType: StatsType,
        @PathVariable baseDate: String,
        @PathVariable entityId: Long,
    ): ResponseEntity<ApiResponse<GetStatsByEntityIdAndBaseDate.Response<*>>> {
        val req =
            GetStatsByEntityIdAndBaseDate.Request(
                statsType = statsType,
                baseDate = BaseDate(baseDate),
                entityId = entityId,
            )
        return ApiResponse.ok(getStatsByEntityIdAndBaseDate(req))
    }
}
