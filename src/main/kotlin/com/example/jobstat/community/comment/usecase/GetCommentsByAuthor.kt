package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.CommentConstants
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
internal class GetCommentsByAuthor(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentsByAuthor.Request, GetCommentsByAuthor.Response>(validator) {
    
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        PageRequest.of(request.page ?: 0, CommentConstants.DEFAULT_PAGE_SIZE).let { pageRequest ->
            commentService.getCommentsByAuthor(request.author, pageRequest)
                .map(::mapToAuthorCommentItem)
                .let { commentsPage ->
                    Response(
                        items = commentsPage,
                        totalCount = commentsPage.totalElements,
                        hasNext = commentsPage.hasNext()
                    )
                }
        }
    
    private fun mapToAuthorCommentItem(comment: Comment): AuthorCommentItem = with(comment) {
        AuthorCommentItem(
            id = id,
            boardId = board.id,
            boardTitle = board.title,
            content = content,
            createdAt = createdAt.toString()
        )
    }
    
    data class Request(
        @field:NotBlank val author: String,
        val page: Int?,
    )
    
    data class Response(
        val items: Page<AuthorCommentItem>,
        val totalCount: Long,
        val hasNext: Boolean,
    )
    
    data class AuthorCommentItem(
        val id: Long,
        val boardId: Long,
        val boardTitle: String,
        val content: String,
        val createdAt: String,
    )
} 