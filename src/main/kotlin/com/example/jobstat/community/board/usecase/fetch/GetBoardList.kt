package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetBoardList(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetBoardList.Request, GetBoardList.Response>(validator) {

    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        PageRequest.of(request.page ?: 0, BoardConstants.DEFAULT_PAGE_SIZE).let { pageRequest ->
            fetchBoardsBasedOnRequest(request, pageRequest)
                .map(::mapToBoardListItem)
                .let { boardsPage ->
                    Response(
                        items = boardsPage,
                        totalCount = boardsPage.totalElements,
                        hasNext = boardsPage.hasNext()
                    )
                }
        }

    private fun fetchBoardsBasedOnRequest(request: Request, pageable: PageRequest): Page<Board> =
        with(request) {
            when {
                categoryId != null -> boardService.getBoardsByCategory(categoryId, pageable)
                author != null -> boardService.getBoardsByAuthor(author, pageable)
                keyword != null -> boardService.searchBoards(keyword, pageable)
                else -> boardService.getAllBoards(pageable)
            }
        }

    private fun mapToBoardListItem(board: Board): ReadBoardListItem = with(board) {
        ReadBoardListItem(
            id = id,
            title = title,
            author = author,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = commentCount,
            categoryId = category.id,
            createdAt = createdAt.toString()
        )
    }

    data class Request(
        val page: Int?,
        val categoryId: Long?,
        val author: String?,
        val keyword: String?,
    )

    data class Response(
        val items: Page<ReadBoardListItem>,
        val totalCount: Long,
        val hasNext: Boolean,
    )

    data class ReadBoardListItem(
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