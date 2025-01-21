package com.example.jobstat.board.usecase

import com.example.jobstat.board.BoardService
import com.example.jobstat.board.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

@Service
class AddCommentToBoard(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<AddCommentToBoard.Request, AddCommentToBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val board =
            boardService.getBoardById(request.boardId)
                ?: throw NoSuchElementException("Board not found")
        val comment = commentService.createComment(request.boardId, request.content, request.author)
        return Response(
            boardId = board.id,
            commentId = comment.id,
            content = comment.content,
            author = comment.author,
            createdAt = comment.createdAt.toString(),
        )
    }

    data class Request(
        @field:Positive
        val boardId: Long,
        @field:NotBlank
        val content: String,
        @field:NotBlank
        val author: String,
    )

    data class Response(
        val boardId: Long,
        val commentId: Long,
        val content: String,
        val author: String,
        val createdAt: String,
    )
}
