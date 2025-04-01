package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetBoardsByCategory(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetBoardsByCategory.Request, GetBoardsByCategory.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        fetchBoardsPage(request).let { boardsPage ->
            Response(
                items = boardsPage,
                totalCount = boardsPage.totalElements,
                hasNext = boardsPage.hasNext()
            )
        }
    
    private fun fetchBoardsPage(request: Request): Page<ReadBoardByCategoryItem> = with(request) {
        val pageable = PageRequest.of(page ?: 0, 20)
        
        when {
            author != null -> boardService.getBoardsByAuthorAndCategory(
                author = author,
                categoryId = categoryId,
                pageable = pageable
            )
            else -> boardService.getBoardsByCategory(
                categoryId = categoryId,
                pageable = pageable
            )
        }.map(::mapToBoardItem)
    }
    
    private fun mapToBoardItem(board: Board): ReadBoardByCategoryItem = with(board) {
        ReadBoardByCategoryItem(
            id = id,
            title = title,
            author = author,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = comments.size,
            createdAt = createdAt.toString()
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
