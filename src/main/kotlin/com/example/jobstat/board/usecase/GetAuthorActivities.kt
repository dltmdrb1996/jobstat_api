package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetAuthorActivities(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetAuthorActivities.Request, GetAuthorActivities.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val boardPage = boardService.getBoardsByAuthor(request.author, PageRequest.of(request.page ?: 0, 10))
        val commentPage = commentService.getCommentsByAuthor(request.author, PageRequest.of(request.page ?: 0, 10))

        val boards: Page<BoardActivity> =
            boardPage.map { board ->
                BoardActivity(
                    id = board.id,
                    title = board.title,
                    viewCount = board.viewCount,
                    likeCount = board.likeCount,
                    commentCount = board.comments.size,
                    createdAt = board.createdAt.toString(),
                )
            }
        val comments: Page<CommentActivity> =
            commentPage.map { comment ->
                CommentActivity(
                    id = comment.id,
                    content = comment.content,
                    boardId = comment.board.id,
                    boardTitle = comment.board.title,
                    createdAt = comment.createdAt.toString(),
                )
            }

        return Response(
            boards = boards,
            comments = comments,
            boardsTotalCount = boardPage.totalElements,
            commentsTotalCount = commentPage.totalElements,
            boardsHasNext = boardPage.hasNext(),
            commentsHasNext = commentPage.hasNext(),
        )
    }

    data class Request(
        @field:NotBlank val author: String,
        val page: Int?,
    )

    data class Response(
        val boards: Page<BoardActivity>,
        val comments: Page<CommentActivity>,
        val boardsTotalCount: Long,
        val commentsTotalCount: Long,
        val boardsHasNext: Boolean,
        val commentsHasNext: Boolean,
    )

    data class BoardActivity(
        val id: Long,
        val title: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val createdAt: String,
    )

    data class CommentActivity(
        val id: Long,
        val content: String,
        val boardId: Long,
        val boardTitle: String,
        val createdAt: String,
    )
}
