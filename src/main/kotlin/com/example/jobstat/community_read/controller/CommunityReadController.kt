package com.example.jobstat.community_read.controller

import com.example.jobstat.community_read.usecase.query.* // 모든 UseCase import
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_web_util.ApiResponse
import com.example.jobstat.core.core_security.annotation.Public
import com.example.jobstat.statistics_read.core.core_model.BoardRankingMetric
import com.example.jobstat.statistics_read.core.core_model.BoardRankingPeriod
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/v1/community/read/boards")
@Tag(name = "게시판 조회(Read)", description = "게시글 및 댓글 조회 관련 API")
@Public
class CommunityReadController(
    private val getBoardDetailById: GetBoardDetailById,
    private val getBoardListByOffsetUseCase: GetBoardListByOffsetUseCase,
    private val getBoardListByCursorUseCase: GetBoardListByCursorUseCase,
    private val getBoardsByIds: GetBoardsByIdsUseCase,
    private val getCommentsByBoardId: GetCommentsByBoardIdUseCase,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{id}")
    @Operation(
        summary = "게시글 상세 조회",
        description = "특정 게시글의 상세 정보를 조회합니다. 조회수 증가 이벤트가 발행될 수 있습니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 상세 조회 성공",
        content = [Content(schema = Schema(implementation = GetBoardDetailById.Response::class))],
    )
    @SwaggerResponse(responseCode = "404", description = "게시글을 찾을 수 없음", content = [Content()])
    fun getBoardById(
        @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "false")
        includeComments: Boolean,
        @Parameter(description = "댓글 페이지 번호", required = false, example = "20")
        @RequestParam(required = false, defaultValue = "20")
        commentPageSize: Int,
    ): ResponseEntity<ApiResponse<GetBoardDetailById.Response>> {
        log.debug("조회 요청: 게시글 ID=$id")
        return ApiResponse.ok(
            getBoardDetailById(
                GetBoardDetailById.Request.create(
                    boardId = id,
                    includeComments = includeComments,
                    commentPageSize = commentPageSize,
                ),
            ),
        )
    }

    @GetMapping
    @Operation(
        summary = "최신 게시글 목록 조회 (Offset)",
        description = "최신 게시글 목록을 페이지 기반(Offset)으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "최신 게시글 목록 조회 성공 (Offset)",
        content = [Content(schema = Schema(implementation = GetBoardListByOffsetUseCase.Response::class))],
    )
    fun getLatestBoardsByOffset(
        @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<GetBoardListByOffsetUseCase.Response>> {
        log.debug("조회 요청: 최신 게시글 목록 (페이지 기반) page=$page, size=$size")
        val request =
            GetBoardListByOffsetUseCase.Request(
                type = "latest",
                period = "all",
                page = page,
                size = size,
            )
        return ApiResponse.ok(getBoardListByOffsetUseCase(request))
    }

    @GetMapping("/after")
    @Operation(
        summary = "최신 게시글 목록 조회 (Cursor)",
        description = "특정 게시글 ID 이후의 최신 게시글 목록을 커서 기반으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "최신 게시글 목록 조회 성공 (Cursor)",
        content = [Content(schema = Schema(implementation = GetBoardListByCursorUseCase.Response::class))],
    )
    fun getLatestBoardsByCursor(
        @Parameter(description = "마지막으로 조회한 게시글 ID (첫 페이지는 null)", example = "100") @RequestParam(required = false) lastId: Long?,
        @Parameter(description = "조회할 개수", example = "20") @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetBoardListByCursorUseCase.Response>> {
        log.debug("조회 요청: 최신 게시글 목록 (커서 기반) lastId=$lastId, limit=$limit")
        val request =
            GetBoardListByCursorUseCase.Request(
                type = "latest",
                period = "all",
                lastId = lastId,
                limit = limit.toLong(),
            )
        return ApiResponse.ok(getBoardListByCursorUseCase(request))
    }

    @GetMapping("/category/{categoryId}")
    @Operation(
        summary = "카테고리별 게시글 목록 조회 (Offset)",
        description = "특정 카테고리의 게시글 목록을 페이지 기반(Offset)으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "카테고리별 게시글 목록 조회 성공 (Offset)",
        content = [Content(schema = Schema(implementation = GetBoardListByOffsetUseCase.Response::class))],
    )
    fun getBoardsByCategoryByOffset(
        @Parameter(description = "카테고리 ID", required = true, example = "1") @PathVariable categoryId: Long,
        @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<GetBoardListByOffsetUseCase.Response>> {
        log.debug("조회 요청: 카테고리 게시글 목록 (페이지 기반) categoryId=$categoryId, page=$page, size=$size")
        val categoryIdStr = categoryId.toString()
        val request =
            GetBoardListByOffsetUseCase.Request(
                type = "category",
                period = categoryIdStr,
                page = page,
                size = size,
            )
        return ApiResponse.ok(getBoardListByOffsetUseCase(request))
    }

    @GetMapping("/category/{categoryId}/after")
    @Operation(
        summary = "카테고리별 게시글 목록 조회 (Cursor)",
        description = "특정 카테고리에서 특정 게시글 ID 이후의 게시글 목록을 커서 기반으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "카테고리별 게시글 목록 조회 성공 (Cursor)",
        content = [Content(schema = Schema(implementation = GetBoardListByCursorUseCase.Response::class))],
    )
    fun getBoardsByCategoryByCursor(
        @Parameter(description = "카테고리 ID", required = true, example = "1") @PathVariable categoryId: Long,
        @Parameter(description = "마지막으로 조회한 게시글 ID (첫 페이지는 null)", example = "100") @RequestParam(required = false) lastId: Long?,
        @Parameter(description = "조회할 개수", example = "20") @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetBoardListByCursorUseCase.Response>> {
        log.debug("조회 요청: 카테고리 게시글 목록 (커서 기반) categoryId=$categoryId, lastId=$lastId, limit=$limit")
        val categoryIdStr = categoryId.toString()
        val request =
            GetBoardListByCursorUseCase.Request(
                type = "category",
                period = categoryIdStr,
                lastId = lastId,
                limit = limit.toLong(),
            )
        return ApiResponse.ok(getBoardListByCursorUseCase(request))
    }

    @GetMapping("/ranking/{metric}/{period}")
    @Operation(
        summary = "랭킹별 게시글 목록 조회 (Offset)",
        description = "지표(좋아요/조회수)와 기간(일/주/월)에 따른 게시글 랭킹을 페이지 기반(Offset)으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "랭킹별 게시글 목록 조회 성공 (Offset)",
        content = [Content(schema = Schema(implementation = GetBoardListByOffsetUseCase.Response::class))],
    )
    @SwaggerResponse(responseCode = "400", description = "유효하지 않은 랭킹 지표 또는 기간", content = [Content()])
    fun getRankedBoardsByOffset(
        @Parameter(description = "랭킹 지표 (LIKES, VIEWS)", required = true, example = "LIKES")
        @PathVariable metric: String,
        @Parameter(description = "랭킹 기간 (DAY, WEEK, MONTH)", required = true, example = "WEEK")
        @PathVariable period: String,
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<GetBoardListByOffsetUseCase.Response>> {
        val metricEnum =
            try {
                BoardRankingMetric.fromString(metric)
            } catch (e: IllegalArgumentException) {
                throw AppException.fromErrorCode(
                    ErrorCode.INVALID_ARGUMENT,
                    detailInfo = "Invalid metric: $metric",
                )
            }
        val periodEnum =
            try {
                BoardRankingPeriod.fromString(period)
            } catch (e: IllegalArgumentException) {
                throw AppException.fromErrorCode(
                    ErrorCode.INVALID_ARGUMENT,
                    detailInfo = "Invalid period: $period",
                )
            }

        val request =
            GetBoardListByOffsetUseCase.Request(
                type = metricEnum.name.lowercase(),
                period = periodEnum.name.lowercase(),
                size = size,
            )
        return ApiResponse.ok(getBoardListByOffsetUseCase(request))
    }

    @GetMapping("/ranking/{metric}/{period}/after")
    @Operation(
        summary = "랭킹별 게시글 목록 조회 (Cursor)",
        description = "지표(좋아요/조회수)와 기간(일/주/월)에 따른 게시글 랭킹을 특정 게시글 ID 이후부터 커서 기반으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "랭킹별 게시글 목록 조회 성공 (Cursor)",
        content = [Content(schema = Schema(implementation = GetBoardListByCursorUseCase.Response::class))],
    )
    @SwaggerResponse(responseCode = "400", description = "유효하지 않은 랭킹 지표 또는 기간", content = [Content()])
    fun getRankedBoardsByCursor(
        @Parameter(description = "랭킹 지표 (LIKES, VIEWS)", required = true, example = "LIKES")
        @PathVariable metric: String,
        @Parameter(description = "랭킹 기간 (DAY, WEEK, MONTH)", required = true, example = "WEEK")
        @PathVariable period: String,
        @Parameter(description = "마지막으로 조회한 게시글 ID (첫 페이지는 null)", example = "100")
        @RequestParam(required = false) lastId: Long?,
        @Parameter(description = "조회할 개수", example = "20")
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetBoardListByCursorUseCase.Response>> {
        log.debug("조회 요청: 랭킹 게시글 목록 (커서 기반) metric=$metric, period=$period, lastId=$lastId, limit=$limit")

        val metricEnum =
            try {
                BoardRankingMetric.fromString(metric)
            } catch (e: IllegalArgumentException) {
                throw AppException.fromErrorCode(
                    ErrorCode.INVALID_ARGUMENT,
                    detailInfo = "Invalid metric: $metric",
                )
            }
        val periodEnum =
            try {
                BoardRankingPeriod.fromString(period)
            } catch (e: IllegalArgumentException) {
                throw AppException.fromErrorCode(
                    ErrorCode.INVALID_ARGUMENT,
                    detailInfo = "Invalid metric: $metric",
                )
            }

        val request =
            GetBoardListByCursorUseCase.Request(
                type = metricEnum.name.lowercase(),
                period = periodEnum.name.lowercase(),
                lastId = lastId,
                limit = limit.toLong(),
            )
        return ApiResponse.ok(getBoardListByCursorUseCase(request))
    }

    @GetMapping("/{boardId}/comments")
    @Operation(summary = "게시글 댓글 목록 조회 (Offset)")
    @SwaggerResponse(
        responseCode = "200",
        description = "댓글 목록 조회 성공",
        content = [Content(schema = Schema(implementation = GetCommentsByBoardIdUseCase.Response::class))],
    )
    @SwaggerResponse(responseCode = "404", description = "게시글을 찾을 수 없음", content = [Content()])
    fun getCommentsByBoardId(
        @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable boardId: Long,
        @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<GetCommentsByBoardIdUseCase.Response>> {
        log.debug("조회 요청: 게시글 댓글 목록 boardId=$boardId, page=$page, size=$size")
        val request =
            GetCommentsByBoardIdUseCase.Request(
                boardId = boardId,
                page = page,
                size = size,
            )
        return ApiResponse.ok(getCommentsByBoardId(request))
    }

    @PostMapping("/bulk")
    @Operation(summary = "게시글 ID 목록으로 게시글 조회")
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 목록 조회 성공",
        content = [Content(schema = Schema(implementation = GetBoardsByIdsUseCase.Response::class))],
    )
    @SwaggerResponse(responseCode = "400", description = "잘못된 요청 (ID 목록 비어있음 등)", content = [Content()])
    fun getBoardsByIds(
        @Parameter(description = "게시글 ID 목록", required = true) @RequestBody request: GetBoardsByIdsUseCase.Request,
    ): ResponseEntity<ApiResponse<GetBoardsByIdsUseCase.Response>> {
        log.debug("조회 요청: 게시글 ID 목록 조회 boardIds=${request.boardIds}")
        return ApiResponse.ok(getBoardsByIds.invoke(request))
    }
}
