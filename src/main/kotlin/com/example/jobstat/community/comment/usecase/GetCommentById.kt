package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetCommentById(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentById.Request, GetCommentById.Response>(validator) {
    
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val comment = commentService.getCommentById(request.commentId)
        return Response.from(comment)
    }
    
    data class Request(
        @field:Positive val commentId: Long,
    )
    
    data class Response(
        val id: Long,
        val boardId: Long,
        val content: String,
        val author: String,
        val createdAt: String,
        val updatedAt: String,
    ) {
        companion object {
            fun from(comment: Comment) = with(comment) {
                Response(
                    id = id,
                    boardId = board.id,
                    content = content,
                    author = author,
                    createdAt = createdAt.toString(),
                    updatedAt = updatedAt.toString()
                )
            }
        }
    }
} 