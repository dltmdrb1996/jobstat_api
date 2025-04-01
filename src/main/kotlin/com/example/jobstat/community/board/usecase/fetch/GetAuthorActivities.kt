package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetAuthorActivities(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetAuthorActivities.Request, GetAuthorActivities.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        val pageRequest = PageRequest.of(page ?: 0, 10)
        val boards = boardService
            .getBoardsByAuthor(request.author, pageRequest)
            .map(BoardActivity.Companion::from)
        val comments = commentService
            .getCommentsByAuthor(request.author, pageRequest)
            .map(CommentActivity.Companion::from)
        Response.from(boards, comments)
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
    ) {
        companion object {
            fun from(
                boards: Page<BoardActivity>,
                comments: Page<CommentActivity>,
            ): Response = Response(
                boards = boards,
                comments = comments,
                boardsTotalCount = boards.totalElements,
                commentsTotalCount = comments.totalElements,
                boardsHasNext = boards.hasNext(),
                commentsHasNext = comments.hasNext(),
            )
        }
    }

    data class BoardActivity(
        val id: Long,
        val title: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val createdAt: String,
    ) {
        companion object {
            fun from(board: Board): BoardActivity = with(board) {
                BoardActivity(
                    id = id,
                    title = title,
                    viewCount = viewCount,
                    likeCount = likeCount,
                    commentCount = comments.size,
                    createdAt = createdAt.toString(),
                )
            }
        }
    }

    data class CommentActivity(
        val id: Long,
        val content: String,
        val boardId: Long,
        val boardTitle: String,
        val createdAt: String,
    ) {
        companion object {
            fun from(comment: Comment): CommentActivity = with(comment) {
                CommentActivity(
                    id = id,
                    content = content,
                    boardId = board.id,
                    boardTitle = board.title,
                    createdAt = createdAt.toString(),
                )
            }
        }
    }
}
