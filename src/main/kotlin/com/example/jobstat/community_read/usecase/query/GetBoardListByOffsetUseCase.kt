// file: src/main/kotlin/com/example/jobstat/community_read/usecase/query/GetTopBoardsByOffsetUseCase.kt
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
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
// import org.springframework.transaction.annotation.Transactional // Read 서비스는 보통 필요 없음

/**
 * 페이지 기반 게시글 목록 조회 유스케이스
 * 오프셋(페이지 번호) 기반으로 게시글 목록을 조회
 */
@Service
class GetBoardListByOffsetUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator,
) : ValidUseCase<GetBoardListByOffsetUseCase.Request, GetBoardListByOffsetUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    // ===================================================
    // 유스케이스 실행 메소드
    // ===================================================

    override fun execute(request: Request): Response {
        log.info(
            "게시글 목록 페이지 기반 조회: type=${request.type}, period=${request.period}, page=${request.page}, size=${request.size}",
        )

        // 페이징 객체 생성
        val pageable = PageRequest.of(request.page, request.size)

        // 조회 유형에 따른 처리
        val boardsPage = executeByListType(request, pageable)

        // 응답 생성
        return Response(
            items = BoardResponseDto.from(boardsPage.content),
            totalCount = boardsPage.totalElements,
            period = request.period, // 응답에는 요청받은 문자열 그대로 전달
            page = request.page,
            size = request.size,
            hasNext = boardsPage.hasNext(),
        )
    }

    // ===================================================
    // 내부 구현 메소드
    // ===================================================

    /**
     * 조회 유형에 따라 적절한 서비스 메소드 호출
     */
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

    // ===================================================
    // 요청 및 응답 모델
    // ===================================================

    /**
     * 게시글 목록 페이지 기반 조회 요청 모델
     */
    @Schema(name = "GetTopBoardsByOffsetRequest", description = "게시글 목록 페이지 기반 조회 요청 모델")
    data class Request(
        @field:Schema(description = "조회 유형 (최신순: latest, 좋아요: likes, 조회수: views, 카테고리: category)", example = "likes", allowableValues = ["latest", "likes", "views", "category"], required = true)
        @field:Pattern(regexp = "^(latest|likes|views|category)$", message = "유효한 조회 유형이 아닙니다.")
        val type: String,
        @field:Schema(description = "기간 (전체: all, 일간: day, 주간: week, 월간: month, 카테고리 ID: 숫자)", example = "week", allowableValues = ["all", "day", "week", "month", "{categoryId}"])
        val period: String, // categoryId 또는 기간 문자열
        @field:Schema(description = "페이지 번호", example = "0", defaultValue = "0", minimum = "0")
        @field:Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
        val page: Int = 0,
        @field:Schema(description = "페이지 크기", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
        @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
        @field:Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
        val size: Int = 20,
    )

    /**
     * 게시글 목록 페이지 기반 조회 응답 모델
     */
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
