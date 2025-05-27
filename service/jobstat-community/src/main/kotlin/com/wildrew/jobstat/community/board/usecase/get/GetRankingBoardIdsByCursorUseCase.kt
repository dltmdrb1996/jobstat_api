package com.wildrew.jobstat.community.board.usecase.get

import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.board.usecase.get.dto.BoardIdsResponse
import com.wildrew.jobstat.community.board.utils.BoardConstants
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

/**
 * 랭킹 기준으로 게시글 ID 목록을 커서 기반 페이징으로 조회하는 유스케이스
 * - 특정 게시글 ID 이후부터 조회수나 좋아요 수 기준으로 인기 게시글 ID 목록 반환
 */
@Service
class GetRankingBoardIdsByCursorUseCase(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetRankingBoardIdsByCursorUseCase.Request, BoardIdsResponse>(validator) {
    override fun execute(request: Request): BoardIdsResponse {
        val ids: List<Long> =
            boardService.getBoardIdsRankedByMetricAfter(
                metric = request.metric,
                period = request.period,
                lastBoardId = request.lastBoardId,
                limit = request.limit,
            )

        val hasNext = ids.size >= request.limit

        return BoardIdsResponse(
            ids = ids.map { it.toString() },
            hasNext = hasNext,
        )
    }

    @Schema(description = "랭킹별 게시글 ID 목록 (커서 기반) 조회 요청")
    data class Request(
        @field:Schema(description = "랭킹 유형 (LIKES: 좋아요 수, VIEWS: 조회수)", required = true, implementation = BoardRankingMetric::class)
        val metric: BoardRankingMetric,
        @field:Schema(description = "기간 (DAY: 일간, WEEK: 주간, MONTH: 월간)", required = true, implementation = BoardRankingPeriod::class)
        val period: BoardRankingPeriod,
        @field:Schema(description = "마지막 게시글 ID (이 ID 이후부터 조회)", required = false)
        val lastBoardId: Long?,
        @field:Schema(description = "조회 개수", example = "20", defaultValue = "20")
        @field:Positive
        @field:Max(100)
        val limit: Int = BoardConstants.DEFAULT_PAGE_SIZE,
    )
}
