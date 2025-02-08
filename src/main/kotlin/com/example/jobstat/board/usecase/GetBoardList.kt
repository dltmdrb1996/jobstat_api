package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
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
    override fun execute(request: Request): Response {
        val boardsPage: Page<ReadBoardListItem> =
            when {
                request.categoryId != null ->
                    boardService.getBoardsByCategory(
                        categoryId = request.categoryId,
                        pageable = PageRequest.of(request.page ?: 0, 20),
                    )
                request.author != null ->
                    boardService.getBoardsByAuthor(
                        author = request.author,
                        pageable = PageRequest.of(request.page ?: 0, 20),
                    )
                request.keyword != null ->
                    boardService.searchBoards(
                        keyword = request.keyword,
                        pageable = PageRequest.of(request.page ?: 0, 20),
                    )
                else ->
                    boardService.getAllBoards(
                        pageable = PageRequest.of(request.page ?: 0, 20),
                    )
            }.map { board ->
                ReadBoardListItem(
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

        return Response(
            items = boardsPage,
            totalCount = boardsPage.totalElements,
            hasNext = boardsPage.hasNext(),
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
