package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.usecase.get.dto.BoardIdsResponse
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * 최신 게시글 ID 목록을 오프셋 기반 페이징으로 조회하는 유스케이스
 * - 페이지 번호와 크기로 게시글 ID 목록 반환
 */
@Service
internal class GetLatestBoardIdsByOffsetUseCase(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetLatestBoardIdsByOffsetUseCase.Request, BoardIdsResponse>(validator) {
    override fun execute(request: Request): BoardIdsResponse {
        val pageable =
            PageRequest.of(
                request.page,
                request.size,
                Sort.by(Sort.Direction.DESC, "id"), // 최신순 정렬
            )

        val page = boardService.getAllBoards(pageable)

        return BoardIdsResponse(page.content.map { it.id.toString() }, page.hasNext())
    }

    @Schema(description = "최신 게시글 ID 목록 (오프셋 기반) 조회 요청")
    data class Request(
        @field:Schema(description = "페이지 번호", example = "0", defaultValue = "0")
        @field:Min(0)
        val page: Int = 0,
        @field:Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @field:Min(1)
        val size: Int = BoardConstants.DEFAULT_PAGE_SIZE,
    )
}
