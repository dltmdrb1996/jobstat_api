package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class AddComment(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<AddComment.Request, AddComment.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val comment =
            commentService.createComment(
                boardId = request.boardId,
                content = request.content,
                author = request.author,
                password = null,
            )
        return Response(
            id = comment.id,
            content = comment.content,
            author = comment.author,
            boardId = comment.board.id,
            createdAt = comment.createdAt.toString(),
        )
    }

    data class Request(
        @field:Positive val boardId: Long,
        @field:NotBlank @field:Size(max = 1000) val content: String,
        @field:NotBlank val author: String,
    )

    data class Response(
        val id: Long,
        val content: String,
        val author: String,
        val boardId: Long,
        val createdAt: String,
    )
}
