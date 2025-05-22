package com.wildrew.app.community.board.usecase.get

import com.wildrew.app.community.board.service.BoardService
import com.wildrew.app.community.board.usecase.get.dto.BoardIdsResponse
import com.wildrew.app.community.board.utils.BoardConstants
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

/**
 * 최신 게시글 ID 목록을 커서 기반 페이징으로 조회하는 유스케이스
 * - 특정 ID 이후부터 게시글 ID 목록 반환
 */
@Service
class GetLatestBoardIdsByCursorUseCase(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetLatestBoardIdsByCursorUseCase.Request, BoardIdsResponse>(validator) {
    override fun execute(request: Request): BoardIdsResponse {
        val ids = boardService.getBoardsAfter(request.lastBoardId, request.limit)
        return BoardIdsResponse(ids.map { it.id.toString() }, ids.size >= request.limit)
    }

    @Schema(description = "최신 게시글 ID 목록 (커서 기반) 조회 요청")
    data class Request(
        @field:Schema(description = "마지막 게시글 ID (이 ID 이후부터 조회)", required = false)
        val lastBoardId: Long?,
        @field:Schema(description = "조회 개수", example = "20", defaultValue = "20")
        @field:Positive
        @field:Max(100)
        val limit: Int = BoardConstants.DEFAULT_PAGE_SIZE,
    )
}
