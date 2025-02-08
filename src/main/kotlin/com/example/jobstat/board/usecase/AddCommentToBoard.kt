package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

@Service
internal class AddCommentToBoard(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<AddCommentToBoard.Request, AddCommentToBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        // 게시글이 없으면 예외를 발생시킵니다
        val board = boardService.getBoardById(request.boardId)
        val comment =
            commentService.createComment(
                boardId = request.boardId,
                content = request.content,
                author = request.author,
                password = null,
            )
        return Response(
            boardId = board.id,
            commentId = comment.id,
            content = comment.content,
            author = comment.author,
            createdAt = comment.createdAt.toString(),
        )
    }

    data class Request(
        @field:Positive val boardId: Long,
        @field:NotBlank val content: String,
        @field:NotBlank val author: String,
    )

    data class Response(
        val boardId: Long,
        val commentId: Long,
        val content: String,
        val author: String,
        val createdAt: String,
    )
}
