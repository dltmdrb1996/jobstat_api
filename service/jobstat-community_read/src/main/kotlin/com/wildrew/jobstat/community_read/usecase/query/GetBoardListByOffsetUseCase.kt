package com.wildrew.jobstat.community_read.usecase.query

import com.wildrew.jobstat.community_read.model.BoardResponseDto
import com.wildrew.jobstat.community_read.service.CommunityReadService
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class GetBoardListByOffsetUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator,
) : ValidUseCase<GetBoardListByOffsetUseCase.Request, GetBoardListByOffsetUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun execute(request: Request): Response {
        log.debug(
            "게시글 목록 페이지 기반 조회: type=${request.type}, period=${request.period}, page=${request.page}, size=${request.size}",
        )

        val pageable = PageRequest.of(request.page, request.size)

        val boardsPage = executeByListType(request, pageable)

        return Response(
            items = BoardResponseDto.from(boardsPage.content),
            totalCount = boardsPage.totalElements,
            period = request.period,
            page = request.page,
            size = request.size,
            hasNext = boardsPage.hasNext(),
        )
    }

    private fun executeByListType(
        request: Request,
        pageable: PageRequest,
    ) = when (request.type.lowercase()) {
        "latest" -> communityReadService.getLatestBoardsByOffset(pageable)
        "category" -> {
            val categoryId =
                request.period.toLongOrNull()
                    ?: throw AppException.fromErrorCode(
                        ErrorCode.INVALID_ARGUMENT,
                        detailInfo = "카테고리 ID는 숫자여야 합니다: ${request.period}",
                    )
            communityReadService.getCategoryBoardsByOffset(categoryId, pageable)
        }
        "likes", "views" -> { // 랭킹 조회 로직 통합
            val metric = BoardRankingMetric.fromString(request.type)
            val periodEnum = BoardRankingPeriod.fromString(request.period)
            communityReadService.getRankedBoardsByOffset(metric, periodEnum, pageable)
        }
        else -> throw AppException.fromErrorCode(
            ErrorCode.INVALID_REQUEST_BODY,
            message = "유효하지 않은 조회 유형입니다. 'latest', 'category', 'likes', 'views' 중 하나여야 합니다.",
            detailInfo = "type: ${request.type}",
        )
    }

    @Schema(name = "GetTopBoardsByOffsetRequest", description = "게시글 목록 페이지 기반 조회 요청 모델")
    data class Request(
        @field:Schema(description = "조회 유형 (최신순: latest, 좋아요: likes, 조회수: views, 카테고리: category)", example = "likes", allowableValues = ["latest", "likes", "views", "category"], required = true)
        @field:Pattern(regexp = "^(latest|likes|views|category)$", message = "유효한 조회 유형이 아닙니다.")
        val type: String,
        @field:Schema(description = "기간 (전체: all, 일간: day, 주간: week, 월간: month, 카테고리 ID: 숫자)", example = "week", allowableValues = ["all", "day", "week", "month", "{categoryId}"])
        val period: String, // categoryId 또는 기간 문자열
        @field:Schema(description = "페이지 번호", example = "0", defaultValue = "0", minimum = "0")
        val page: Int = 0,
        @field:Schema(description = "페이지 크기", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
        @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
        @field:Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
        val size: Int = 20,
    )

    @Schema(name = "GetTopBoardsByOffsetResponse", description = "게시글 목록 페이지 기반 조회 응답 모델")
    data class Response(
        @field:Schema(description = "게시글 목록") val items: List<BoardResponseDto>,
        @field:Schema(description = "총 게시글 수", example = "42") val totalCount: Long,
        @field:Schema(description = "조회 기간/카테고리ID", example = "week") val period: String,
        @field:Schema(description = "현재 페이지", example = "0") val page: Int,
        @field:Schema(description = "페이지 크기", example = "20") val size: Int,
        @field:Schema(description = "다음 페이지 존재 여부", example = "true") val hasNext: Boolean,
    )
}
