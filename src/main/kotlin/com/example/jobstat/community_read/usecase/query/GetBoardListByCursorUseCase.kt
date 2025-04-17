package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.model.BoardResponseDto
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.state.BoardRankingMetric // Enum 임포트
import com.example.jobstat.core.state.BoardRankingPeriod // Enum 임포트
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetBoardListByCursorUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator,
) : ValidUseCase<GetBoardListByCursorUseCase.Request, GetBoardListByCursorUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun execute(request: Request): Response {
        log.info(
            "게시글 목록 커서 기반 조회: type=${request.type}, period=${request.period}, lastId=${request.lastId}, limit=${request.limit}",
        )

        val result = executeByListType(request)

        val hasNext = result.size >= request.limit
        val nextCursor = if (hasNext) result.lastOrNull()?.id else null

        return Response(
            items = BoardResponseDto.from(result),
            hasNext = hasNext,
            nextCursor = nextCursor,
            period = request.period,
        )
    }

    private fun executeByListType(request: Request) =
        when (request.type.lowercase()) {
            "latest" -> communityReadService.getLatestBoardsByCursor(request.lastId, request.limit)
            "category" -> {
                val categoryId =
                    request.period.toLongOrNull()
                        ?: throw AppException.fromErrorCode(
                            ErrorCode.INVALID_ARGUMENT,
                            detailInfo = "카테고리 ID는 숫자여야 합니다: ${request.period}",
                        )
                communityReadService.getCategoryBoardsByCursor(categoryId, request.lastId, request.limit)
            }
            "likes", "views" -> { // 랭킹 조회 로직 통합
                val metric = BoardRankingMetric.fromString(request.type)
                val periodEnum = BoardRankingPeriod.fromString(request.period)
                communityReadService.getRankedBoardsByCursor(
                    metric = metric,
                    period = periodEnum,
                    lastBoardId = request.lastId,
                    limit = request.limit,
                )
            }
            else -> throw AppException.fromErrorCode(
                ErrorCode.INVALID_REQUEST_BODY,
                message = "유효하지 않은 조회 유형입니다. 'latest', 'category', 'likes', 'views' 중 하나여야 합니다.",
                detailInfo = "type: ${request.type}",
            )
        }

    @Schema(name = "GetTopBoardsByCursorRequest", description = "게시글 목록 커서 기반 조회 요청 모델")
    data class Request(
        @field:Schema(description = "조회 유형 (최신순: latest, 좋아요: likes, 조회수: views, 카테고리: category)", example = "likes", allowableValues = ["latest", "likes", "views", "category"], required = true)
        @field:Pattern(regexp = "^(latest|likes|views|category)$", message = "유효한 조회 유형이 아닙니다.")
        val type: String,
        @field:Schema(description = "기간 (전체: all, 일간: day, 주간: week, 월간: month, 카테고리 ID: 숫자)", example = "week", allowableValues = ["all", "day", "week", "month", "{categoryId}"])
        val period: String, // categoryId 또는 기간 문자열
        @field:Schema(description = "마지막으로 조회한 게시글 ID (첫 페이지는 null)", example = "100")
        @field:Positive(message = "마지막 게시글 ID는 양수여야 합니다")
        val lastId: Long?, // Nullable로 변경
        @field:Schema(description = "조회할 게시글 개수", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
        @field:Min(value = 1, message = "조회 개수는 1 이상이어야 합니다")
        @field:Max(value = 100, message = "조회 개수는 100 이하여야 합니다")
        val limit: Long = 20,
    )

    @Schema(name = "GetTopBoardsByCursorResponse", description = "게시글 목록 커서 기반 조회 응답 모델")
    data class Response(
        @field:Schema(description = "게시글 목록") val items: List<BoardResponseDto>,
        @field:Schema(description = "다음 데이터 존재 여부", example = "true") val hasNext: Boolean,
        @field:Schema(description = "다음 조회 시 사용할 커서 (마지막 게시글 ID)", example = "95") val nextCursor: Long?,
        @field:Schema(description = "조회 기간/카테고리ID", example = "week") val period: String,
    )
}
