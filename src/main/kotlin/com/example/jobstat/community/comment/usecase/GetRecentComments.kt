package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.CommentConstants
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetRecentComments(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetRecentComments.Request, GetRecentComments.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val comments =
            commentService.getRecentCommentsByBoardId(
                request.boardId,
                CommentConstants.DEFAULT_RECENT_COMMENTS_LIMIT,
            )
        val items =
            comments.map { comment ->
                RecentCommentResponse(
                    id = comment.id,
                    content = comment.content,
                    author = comment.author,
                    createdAt = comment.createdAt.toString(),
                )
            }
        return Response(items = items)
    }

    data class Request(
        @field:Positive val boardId: Long,
    )

    data class Response(
        val items: List<RecentCommentResponse>,
    )

    data class RecentCommentResponse(
        val id: Long,
        val content: String,
        val author: String,
        val createdAt: String,
    )
}
