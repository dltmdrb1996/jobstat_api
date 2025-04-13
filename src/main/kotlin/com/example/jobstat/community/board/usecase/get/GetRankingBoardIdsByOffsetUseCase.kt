// file: src/main/kotlin/com/example/jobstat/community/board/usecase/get/GetRankingBoardIdsByOffsetUseCase.kt
package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.usecase.get.dto.BoardIdsResponse
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetRankingBoardIdsByOffsetUseCase(
    private val boardService: BoardService,
    validator: Validator
) : ValidUseCase<GetRankingBoardIdsByOffsetUseCase.Request, BoardIdsResponse>(validator) {

    @Transactional(readOnly = true)
    override fun invoke(request: Request): BoardIdsResponse {
        return super.invoke(request)
    }

    override fun execute(request: Request): BoardIdsResponse {

        val pageable: Pageable = PageRequest.of(request.page, request.size)

        val resultPage: Page<Long> = boardService.getBoardIdsRankedByMetric(
            metric = request.metric, // Pass enum
            period = request.period, // Pass enum
            pageable = pageable
        )

        return BoardIdsResponse(
            ids = resultPage.content.map { it.toString() },
            hasNext = resultPage.hasNext()
        )
    }

    @Schema(description = "랭킹별 게시글 ID 목록 (Offset) 조회 요청")
    data class Request(
        @field:Schema(description = "랭킹 유형 (LIKES, VIEWS)", required = true, implementation = BoardRankingMetric::class)
        val metric: BoardRankingMetric, // Changed to Enum

        @field:Schema(description = "기간 (DAY, WEEK, MONTH)", required = true, implementation = BoardRankingPeriod::class)
        val period: BoardRankingPeriod,     // Changed to Enum

        @field:Schema(description = "페이지 번호", example = "0", defaultValue = "0")
        @field:Min(0)
        val page: Int = 0,

        @field:Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @field:Min(1)
        val size: Int = BoardConstants.DEFAULT_PAGE_SIZE
    )
}