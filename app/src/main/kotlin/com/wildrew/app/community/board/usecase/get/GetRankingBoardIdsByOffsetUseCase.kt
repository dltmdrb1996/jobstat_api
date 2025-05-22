package com.wildrew.app.community.board.usecase.get

import com.wildrew.app.community.board.service.BoardService
import com.wildrew.app.community.board.usecase.get.dto.BoardIdsResponse
import com.wildrew.app.community.board.utils.BoardConstants
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 랭킹 기준으로 게시글 ID 목록을 오프셋 기반 페이징으로 조회하는 유스케이스
 * - 조회수나 좋아요 수를 기준으로 특정 기간 내 인기 게시글 ID 목록 반환
 */
@Service
class GetRankingBoardIdsByOffsetUseCase(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetRankingBoardIdsByOffsetUseCase.Request, BoardIdsResponse>(validator) {
    override fun execute(request: Request): BoardIdsResponse {
        val pageable: Pageable = PageRequest.of(request.page, request.size)

        val resultPage: Page<Long> =
            boardService.getBoardIdsRankedByMetric(
                metric = request.metric,
                period = request.period,
                pageable = pageable,
            )

        return BoardIdsResponse(
            ids = resultPage.content.map { it.toString() },
            hasNext = resultPage.hasNext(),
        )
    }

    @Schema(description = "랭킹별 게시글 ID 목록 (오프셋 기반) 조회 요청")
    data class Request(
        @field:Schema(description = "랭킹 유형 (LIKES: 좋아요 수, VIEWS: 조회수)", required = true, implementation = BoardRankingMetric::class)
        val metric: BoardRankingMetric,
        @field:Schema(description = "기간 (DAY: 일간, WEEK: 주간, MONTH: 월간)", required = true, implementation = BoardRankingPeriod::class)
        val period: BoardRankingPeriod,
        @field:Schema(description = "페이지 번호", example = "0", defaultValue = "0")
        @field:Min(0)
        val page: Int = 0,
        @field:Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @field:Min(1)
        val size: Int = BoardConstants.DEFAULT_PAGE_SIZE,
    )
}
