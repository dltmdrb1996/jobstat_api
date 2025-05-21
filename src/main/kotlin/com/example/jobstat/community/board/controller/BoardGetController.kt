package com.example.jobstat.community.board.controller

import com.example.jobstat.community.board.usecase.get.* // 분리된 UseCase들 import
import com.example.jobstat.community.board.usecase.get.dto.BoardIdsResponse // 공통 응답 DTO import
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.core.core_web_util.constant.RestConstants
import com.example.jobstat.core.core_web_util.ApiResponse
import com.example.jobstat.core.core_security.annotation.Public
import com.example.jobstat.statistics_read.core.core_model.BoardRankingMetric
import com.example.jobstat.statistics_read.core.core_model.BoardRankingPeriod
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}")
@Tag(name = "게시판 조회(Command서버)", description = "게시글 Fetch 관련 API")
internal class BoardGetController(
    private val getBoardById: GetBoardById,
    private val getBoardsByIds: GetBoardsByIds,
    // --- ID 목록 조회 UseCases (Offset) ---
    private val getLatestBoardIdsByOffsetUseCase: GetLatestBoardIdsByOffsetUseCase,
    private val getCategoryBoardIdsByOffsetUseCase: GetCategoryBoardIdsByOffsetUseCase,
    private val getAuthorBoardIdsByOffsetUseCase: GetAuthorBoardIdsByOffsetUseCase,
    private val getRankingBoardIdsByOffsetUseCase: GetRankingBoardIdsByOffsetUseCase,
    // --- ID 목록 조회 UseCases (Cursor) ---
    private val getLatestBoardIdsByCursorUseCase: GetLatestBoardIdsByCursorUseCase,
    private val getCategoryBoardIdsByCursorUseCase: GetCategoryBoardIdsByCursorUseCase,
    private val getAuthorBoardIdsByCursorUseCase: GetAuthorBoardIdsByCursorUseCase,
    private val getRankingBoardIdsByCursorUseCase: GetRankingBoardIdsByCursorUseCase,
) {
    @Public
    @GetMapping("/boards/{boardId}")
    @Operation(
        summary = "게시글 상세 조회",
        description = "특정 게시글의 상세 정보를 조회합니다. 게시글의 콘텐츠, 작성자 정보, 댓글 등 모든 정보가 포함됩니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 조회 성공",
        content = [Content(schema = Schema(implementation = GetBoardById.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun fetchBoardDetail(
        @Parameter(description = "조회할 게시글 ID", required = true, example = "1") @PathVariable boardId: Long,
        @Parameter(description = "댓글 페이지 번호", required = false, example = "0") @RequestParam(required = false) commentPage: Int?,
    ): ResponseEntity<ApiResponse<GetBoardById.Response>> {
        val request = GetBoardById.Request(boardId = boardId) // 추가 필드가 필요하다면 수정
        return ApiResponse.ok(getBoardById(request))
    }

    @Public
    @PostMapping("/boards/bulk")
    @Operation(
        summary = "게시글 벌크 조회",
        description = "여러 게시글을 ID 목록으로 한 번에 조회합니다. 게시글 목록 화면에서 캐시된 ID 목록으로 데이터를 불러올 때 사용합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 벌크 조회 성공",
        content = [Content(schema = Schema(implementation = GetBoardsByIds.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "잘못된 요청 (유효성 검증 실패)",
        content = [Content(mediaType = "application/json")],
    )
    fun fetchBoardsByIds(
        @Parameter(description = "조회할 게시글 ID 목록", example = "[1, 2, 3, 4, 5]") @RequestBody request: GetBoardsByIds.Request,
    ): ResponseEntity<ApiResponse<GetBoardsByIds.Response>> = ApiResponse.ok(getBoardsByIds(request))

    @Public
    @GetMapping("/boards-fetch/ids")
    @Operation(
        summary = "게시글 ID 목록 조회 (Offset)",
        description = "게시글 ID 목록만 조회합니다. 캐시 구성을 위해 사용되며, 일반적인 페이지네이션 방식(Offset)을 사용합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 ID 목록 조회 성공",
        content = [Content(schema = Schema(implementation = BoardIdsResponse::class))],
    )
    fun fetchBoardIds(
        @Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) page: Int?,
        @Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false) size: Int?,
        @Parameter(description = "카테고리 ID", example = "1") @RequestParam(required = false) categoryId: Long?,
        @Parameter(description = "작성자", example = "홍길동") @RequestParam(required = false) author: String?,
    ): ResponseEntity<ApiResponse<BoardIdsResponse>> =
        when {
            categoryId != null -> {
                val request =
                    GetCategoryBoardIdsByOffsetUseCase.Request(
                        categoryId,
                        page ?: 0,
                        size ?: BoardConstants.DEFAULT_PAGE_SIZE,
                    )
                ApiResponse.ok(getCategoryBoardIdsByOffsetUseCase(request))
            }
            author != null -> {
                val request =
                    GetAuthorBoardIdsByOffsetUseCase.Request(
                        author,
                        page ?: 0,
                        size ?: BoardConstants.DEFAULT_PAGE_SIZE,
                    )
                ApiResponse.ok(getAuthorBoardIdsByOffsetUseCase(request))
            }
            else -> { // 기본: 최신순
                val request =
                    GetLatestBoardIdsByOffsetUseCase.Request(
                        page ?: 0,
                        size ?: BoardConstants.DEFAULT_PAGE_SIZE,
                    )
                ApiResponse.ok(getLatestBoardIdsByOffsetUseCase(request))
            }
        }

    @Public
    @GetMapping("/boards-fetch/ids/after")
    @Operation(
        summary = "특정 ID 이후 게시글 ID 목록 조회 (Cursor)",
        description = "특정 게시글 ID 이후의 게시글 ID 목록을 조회합니다. 무한 스크롤 방식의 페이징을 위해 커서 기반으로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 ID 목록 커서 조회 성공",
        content = [Content(schema = Schema(implementation = BoardIdsResponse::class))],
    )
    fun fetchBoardIdsAfter(
        @Parameter(description = "마지막 게시글 ID", example = "100") @RequestParam(required = false) lastBoardId: Long?,
        @Parameter(description = "조회 개수", example = "20") @RequestParam(required = false) limit: Int?,
        @Parameter(description = "카테고리 ID", example = "1") @RequestParam(required = false) categoryId: Long?,
        @Parameter(description = "작성자", example = "홍길동") @RequestParam(required = false) author: String?,
    ): ResponseEntity<ApiResponse<BoardIdsResponse>> =
        when {
            categoryId != null -> {
                val request =
                    GetCategoryBoardIdsByCursorUseCase.Request(
                        categoryId,
                        lastBoardId,
                        limit ?: BoardConstants.DEFAULT_PAGE_SIZE,
                    )
                ApiResponse.ok(getCategoryBoardIdsByCursorUseCase(request))
            }
            author != null -> {
                val request =
                    GetAuthorBoardIdsByCursorUseCase.Request(
                        author,
                        lastBoardId,
                        limit ?: BoardConstants.DEFAULT_PAGE_SIZE,
                    )
                ApiResponse.ok(getAuthorBoardIdsByCursorUseCase(request))
            }
            else -> { // 기본: 최신순
                val request =
                    GetLatestBoardIdsByCursorUseCase.Request(
                        lastBoardId,
                        limit ?: BoardConstants.DEFAULT_PAGE_SIZE,
                    )
                ApiResponse.ok(getLatestBoardIdsByCursorUseCase(request))
            }
        }

    // --- 랭킹별 게시글 ID 목록 조회 (Offset) ---
    @Public
    @GetMapping("/boards-fetch/ranks/{metric}/{period}/ids")
    @Operation(
        summary = "랭킹별 게시글 ID 목록 조회 (Offset)",
        description = "특정 랭킹 기준(좋아요 수, 조회수)과 기간에 따른 게시글 ID 목록을 조회합니다. 일반적인 페이지네이션 방식(Offset)을 사용합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "랭킹별 게시글 ID 목록 조회 성공",
        content = [Content(schema = Schema(implementation = BoardIdsResponse::class))],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "유효하지 않은 랭킹 유형 또는 기간",
        content = [Content(mediaType = "application/json")],
    )
    fun fetchRankingBoardIds(
        @Parameter(description = "랭킹 유형 (LIKES, VIEWS)", example = "LIKES", required = true)
        @PathVariable metric: String,
        @Parameter(description = "기간 (DAY, WEEK, MONTH)", example = "WEEK", required = true)
        @PathVariable period: String,
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(required = false) page: Int?,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(required = false) size: Int?,
    ): ResponseEntity<ApiResponse<BoardIdsResponse>> {
        val metricEnum =
            try {
                BoardRankingMetric.fromString(metric)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid metric: $metric")
            }
        val periodEnum =
            try {
                BoardRankingPeriod.fromString(period)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid period: $period")
            }

        val request =
            GetRankingBoardIdsByOffsetUseCase.Request(
                metric = metricEnum,
                period = periodEnum,
                page = page ?: 0,
                size = size ?: BoardConstants.DEFAULT_PAGE_SIZE,
            )
        return ApiResponse.ok(getRankingBoardIdsByOffsetUseCase(request))
    }

    @Public
    @GetMapping("/boards-fetch/ranks/{metric}/{period}/ids/after")
    @Operation(
        summary = "랭킹별 특정 ID 이후 게시글 ID 목록 조회 (Cursor - ID 기반)",
        description = "특정 랭킹 기준(좋아요 수, 조회수)과 기간에 따른 게시글 ID 목록을 마지막 ID 이후부터 조회합니다. 무한 스크롤 방식의 페이징을 위해 ID 기반 커서로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "랭킹별 게시글 ID 목록 커서 조회 성공",
        content = [Content(schema = Schema(implementation = BoardIdsResponse::class))],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "유효하지 않은 랭킹 유형 또는 기간",
        content = [Content(mediaType = "application/json")],
    )
    fun fetchRankingBoardIdsAfter(
        @Parameter(description = "랭킹 유형 (LIKES, VIEWS)", example = "LIKES", required = true)
        @PathVariable metric: String,
        @Parameter(description = "기간 (DAY, WEEK, MONTH)", example = "WEEK", required = true)
        @PathVariable period: String,
        @Parameter(description = "마지막 게시글 ID (페이징 기준)", example = "100")
        @RequestParam(required = false) lastBoardId: Long?,
        @Parameter(description = "조회 개수", example = "20")
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<ApiResponse<BoardIdsResponse>> {
        val metricEnum =
            try {
                BoardRankingMetric.fromString(metric)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid metric: $metric")
            }
        val periodEnum =
            try {
                BoardRankingPeriod.fromString(period)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid period: $period")
            }

        val request =
            GetRankingBoardIdsByCursorUseCase.Request(
                metric = metricEnum,
                period = periodEnum,
                lastBoardId = lastBoardId,
                limit = limit ?: BoardConstants.DEFAULT_PAGE_SIZE,
            )
        return ApiResponse.ok(getRankingBoardIdsByCursorUseCase(request))
    }
}
