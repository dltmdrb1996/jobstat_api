package com.wildrew.jobstat.statistics_read

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import com.wildrew.jobstat.core.core_web_util.RestConstants
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import com.wildrew.jobstat.statistics_read.stats.usecase.GetLatestStats
import com.wildrew.jobstat.statistics_read.stats.usecase.GetStatsByEntityId
import com.wildrew.jobstat.statistics_read.stats.usecase.GetStatsByEntityIdAndBaseDate
import com.wildrew.jobstat.statistics_read.stats.usecase.GetStatsByEntityIds
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

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
    @Operation(
        summary = "엔티티별 통계 조회",
        description = "특정 엔티티의 전체 통계 데이터를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetStatsByEntityId.Response::class),
                    ),
                ],
            ),
        ],
    )
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
    @Operation(
        summary = "다중 엔티티 통계 조회",
        description = "여러 엔티티의 특정 시점 통계 데이터를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetStatsByEntityIds.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getStatsByEntityIds(
        @PathVariable statsType: StatsType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
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
    @Operation(
        summary = "최신 통계 조회",
        description = "특정 엔티티의 최신 통계 데이터를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetLatestStats.Response::class),
                    ),
                ],
            ),
        ],
    )
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
    @Operation(
        summary = "시점별 통계 조회",
        description = "특정 엔티티의 특정 시점 통계 데이터를 조회합니다.",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetStatsByEntityIdAndBaseDate.Response::class),
                    ),
                ],
            ),
        ],
    )
    fun getStatsByEntityIdAndBaseDate(
        @PathVariable statsType: StatsType,
        @Parameter(
            description = "기준 날짜 (YYYYMM)",
            schema = Schema(defaultValue = "202501", type = "string"),
            example = "202501",
        )
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
