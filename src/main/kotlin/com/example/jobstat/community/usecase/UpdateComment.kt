package com.example.jobstat.community.usecase

import com.example.jobstat.community.internal.service.CommentService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class UpdateComment(
    private val commentService: CommentService,
    private val bcryptPasswordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<UpdateComment.ExecuteRequest, UpdateComment.Response>(validator) {
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
            require(comment.userId == currentUserId) { "본인의 댓글만 수정할 수 있습니다" }
        }

        val updated =
            commentService.updateComment(
                id = request.commentId,
                content = request.content,
            )

        return Response(
            id = updated.id,
            content = updated.content,
            author = updated.author,
            createdAt = updated.createdAt.toString(),
            updatedAt = updated.updatedAt.toString(),
        )
    }

    data class Request(
        @field:NotBlank
        @field:Size(max = 1000)
        val content: String,
        @field:Size(min = 4, max = 10)
        val password: String?,
    ) {
        fun of(commentId: Long) =
            ExecuteRequest(
                commentId = commentId,
                content = this.content,
                password = this.password,
            )
    }

    data class ExecuteRequest(
        @field:Positive
        val commentId: Long,
        @field:NotBlank
        @field:Size(max = 1000)
        val content: String,
        @field:Size(min = 4, max = 10)
        val password: String?,
    )

    data class Response(
        val id: Long,
        val content: String,
        val author: String,
        val createdAt: String,
        val updatedAt: String,
    )
}
