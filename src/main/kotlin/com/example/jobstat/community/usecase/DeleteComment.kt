package com.example.jobstat.community.usecase

import com.example.jobstat.community.internal.service.CommentService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class DeleteComment(
    private val commentService: CommentService,
    private val bcryptPasswordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<DeleteComment.ExecuteRequest, DeleteComment.Response>(validator) {
    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        val comment = commentService.getCommentById(request.commentId)

        if (comment.password != null) {
            if (request.password == null) throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            if (!bcryptPasswordUtil.matches(request.password, comment.password!!)) {
                throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            }
        } else {
            val currentUserId =
                securityUtils.getCurrentUserId()
                    ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            require(comment.userId == currentUserId) { "본인의 댓글만 삭제할 수 있습니다" }
        }

        commentService.deleteComment(request.commentId)
        return Response(success = true)
    }

    data class Request(
        @field:Size(min = 4, max = 10)
        val password: String?,
    ) {
        fun of(
            boardId: Long,
            commentId: Long,
        ) = ExecuteRequest(
            boardId = boardId,
            commentId = commentId,
            password = password,
        )
    }

    data class ExecuteRequest(
        @field:Positive
        val boardId: Long,
        @field:Positive
        val commentId: Long,
        @field:Size(min = 4, max = 10)
        val password: String?,
    )

    data class Response(
        val success: Boolean,
    )
}
