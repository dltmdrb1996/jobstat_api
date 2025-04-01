package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetCommentsByIds(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentsByIds.Request, GetCommentsByIds.Response>(validator) {

    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        commentService.getCommentsByIds(request.commentIds)
            .map(::mapToCommentItem)
            .let { Response(it) }

    private fun mapToCommentItem(comment: Comment): CommentItem = with(comment) {
        CommentItem(
            id = id,
            boardId = board.id,
            content = content,
            author = author,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )
    }

    data class Request(
        @field:NotEmpty(message = "댓글 ID 목록은 비어있을 수 없습니다") 
        val commentIds: List<Long>
    )

    data class Response(
        val comments: List<CommentItem>
    )

    data class CommentItem(
        val id: Long,
        val boardId: Long,
        val content: String,
        val author: String,
        val createdAt: String,
        val updatedAt: String,
    )
} 