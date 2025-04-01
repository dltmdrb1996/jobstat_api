package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetTopBoards(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetTopBoards.Request, GetTopBoards.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        boardService.getTopNBoardsByViews(request.limit)
            .map(::mapToTopBoardResponse)
            .let(GetTopBoards::Response)
    
    private fun mapToTopBoardResponse(board: Board): TopBoardResponse = with(board) {
        TopBoardResponse(
            id = id,
            title = title,
            author = author,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = comments.size,
            categoryId = category.id,
            createdAt = createdAt.toString(),
        )
    }

    data class Request(
        @field:Positive
        @field:Max(BoardConstants.MAX_POPULAR_BOARDS_LIMIT.toLong())
        val limit: Int = 10,
    )

    data class Response(
        val items: List<TopBoardResponse>,
    )

    data class TopBoardResponse(
        val id: Long,
        val title: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val categoryId: Long,
        val createdAt: String,
    )
}
