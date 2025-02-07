package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetBoardsByCategory(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetBoardsByCategory.Request, GetBoardsByCategory.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val boardsPage: Page<ReadBoardByCategoryItem> =
            if (request.author != null) {
                boardService.getBoardsByAuthorAndCategory(
                    author = request.author,
                    categoryId = request.categoryId,
                    pageable = PageRequest.of(request.page ?: 0, 20),
                )
            } else {
                boardService.getBoardsByCategory(
                    categoryId = request.categoryId,
                    pageable = PageRequest.of(request.page ?: 0, 20),
                )
            }.map { board ->
                ReadBoardByCategoryItem(
                    id = board.id,
                    title = board.title,
                    author = board.author,
                    viewCount = board.viewCount,
                    likeCount = board.likeCount,
                    commentCount = board.comments.size,
                    createdAt = board.createdAt.toString(),
                )
            }

        return Response(
            items = boardsPage,
            totalCount = boardsPage.totalElements,
            hasNext = boardsPage.hasNext(),
        )
    }

    data class Request(
        @field:Positive val categoryId: Long,
        val author: String?,
        val page: Int?,
    )

    data class Response(
        val items: Page<ReadBoardByCategoryItem>,
        val totalCount: Long,
        val hasNext: Boolean,
    )

    data class ReadBoardByCategoryItem(
        val id: Long,
        val title: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val createdAt: String,
    )
}
