package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.CommentConstants
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetCommentsByBoardId(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentsByBoardId.Request, GetCommentsByBoardId.Response>(validator) {
    
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        PageRequest.of(request.page ?: 0, CommentConstants.DEFAULT_PAGE_SIZE).let { pageRequest ->
            commentService.getCommentsByBoardId(request.boardId, pageRequest)
                .map(::mapToCommentListItem)
                .let { commentsPage ->
                    Response(
                        items = commentsPage,
                        totalCount = commentsPage.totalElements,
                        hasNext = commentsPage.hasNext()
                    )
                }
        }
    
    private fun mapToCommentListItem(comment: Comment): CommentListItem = with(comment) {
        CommentListItem(
            id = id,
            content = content,
            author = author,
            createdAt = createdAt.toString()
        )
    }
    
    data class Request(
        @field:Positive val boardId: Long,
        val page: Int?,
    )
    
    data class Response(
        val items: Page<CommentListItem>,
        val totalCount: Long,
        val hasNext: Boolean,
    )
    
    data class CommentListItem(
        val id: Long,
        val content: String,
        val author: String,
        val createdAt: String,
    )
} 