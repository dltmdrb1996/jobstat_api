package com.example.jobstat.community.usecase

import com.example.jobstat.community.internal.service.BoardService
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
    override fun execute(request: Request): Response {
        val boards = boardService.getTopNBoardsByViews(request.limit)
        val items =
            boards.map { board ->
                TopBoardResponse(
                    id = board.id,
                    title = board.title,
                    author = board.author,
                    viewCount = board.viewCount,
                    likeCount = board.likeCount,
                    commentCount = board.comments.size,
                    categoryId = board.category.id,
                    createdAt = board.createdAt.toString(),
                )
            }
        return Response(items = items)
    }

    data class Request(
        @field:Positive @field:Max(100) val limit: Int = 10,
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
