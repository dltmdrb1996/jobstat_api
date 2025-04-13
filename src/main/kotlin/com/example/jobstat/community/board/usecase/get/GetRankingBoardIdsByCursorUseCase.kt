// file: src/main/kotlin/com/example/jobstat/community/board/usecase/get/GetRankingBoardIdsByCursorUseCase.kt
package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.usecase.get.dto.BoardIdsResponse
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetRankingBoardIdsByCursorUseCase(
    private val boardService: BoardService,
    validator: Validator
) : ValidUseCase<GetRankingBoardIdsByCursorUseCase.Request, BoardIdsResponse>(validator) {

    @Transactional(readOnly = true)
    override fun invoke(request: Request): BoardIdsResponse {
        return super.invoke(request)
    }

    override fun execute(request: Request): BoardIdsResponse {

        val ids: List<Long> = boardService.getBoardIdsRankedByMetricAfter( // lastScore 인자 제거됨 (BoardService 인터페이스 및 구현체 변경 필요)
            metric = request.metric,
            period = request.period,
            lastBoardId = request.lastBoardId,
            // lastScore = request.lastScore, // 제거
            limit = request.limit
        )

        // hasNext 로직은 limit 기준으로 유지
        val hasNext = ids.size >= request.limit

        return BoardIdsResponse(
            ids = ids.map { it.toString() },
            hasNext = hasNext
        )
    }

    @Schema(description = "랭킹별 게시글 ID 목록 (Cursor - ID 기반) 조회 요청") // 설명 수정
    data class Request(
        @field:Schema(description = "랭킹 유형 (LIKES, VIEWS)", required = true, implementation = BoardRankingMetric::class)
        val metric: BoardRankingMetric, // Changed to Enum

        @field:Schema(description = "기간 (DAY, WEEK, MONTH)", required = true, implementation = BoardRankingPeriod::class)
        val period: BoardRankingPeriod,   // Changed to Enum

        @field:Schema(description = "마지막 게시글 ID (페이징 기준)", required = false) // 설명 수정
        val lastBoardId: Long?,

        // lastScore 필드 제거
        // @field:Schema(description = "마지막 게시글 점수 (페이징 기준)", required = false)
        // val lastScore: Double?,

        @field:Schema(description = "조회 개수", example = "20", defaultValue = "20")
        @field:Positive @field:Max(100) // Limit validation remains useful
        val limit: Int = BoardConstants.DEFAULT_PAGE_SIZE
    )
}