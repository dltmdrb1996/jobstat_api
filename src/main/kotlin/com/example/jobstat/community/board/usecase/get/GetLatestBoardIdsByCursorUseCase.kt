package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.usecase.get.dto.BoardIdsResponse
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

@Service
internal class GetLatestBoardIdsByCursorUseCase(
    private val boardService: BoardService,
    validator: Validator
) : ValidUseCase<GetLatestBoardIdsByCursorUseCase.Request, BoardIdsResponse>(validator) {

    override fun execute(request: Request): BoardIdsResponse {
        val ids = boardService.getBoardsAfter(request.lastBoardId, request.limit)
        return BoardIdsResponse(ids.map { it.id.toString() }, ids.size >= request.limit)
    }

    @Schema(description = "최신 게시글 ID 목록 (Cursor) 조회 요청")
    data class Request(
        @field:Schema(description = "마지막 게시글 ID", required = false) // 첫 페이지는 null
        val lastBoardId: Long?,

        @field:Schema(description = "조회 개수", example = "20", defaultValue = "20")
        @field:Positive @field:Max(100)
        val limit: Int = BoardConstants.DEFAULT_PAGE_SIZE
    )
}