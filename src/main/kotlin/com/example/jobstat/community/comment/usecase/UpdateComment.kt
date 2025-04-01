package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.CommentConstants
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class UpdateComment(
    private val commentService: CommentService,
    private val passwordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<UpdateComment.ExecuteRequest, UpdateComment.Response>(validator) {
    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        // 댓글 조회 및 권한 검증
        commentService.getCommentById(request.commentId).also { comment ->
            validatePermission(comment, request.password)
        }

        // 댓글 업데이트 및 응답 변환
        return commentService.updateComment(
            id = request.commentId,
            content = request.content,
        ).let { updated ->
            Response(
                id = updated.id,
                content = updated.content,
                author = updated.author,
                createdAt = updated.createdAt.toString(),
                updatedAt = updated.updatedAt.toString(),
            )
        }
    }

    private fun validatePermission(comment: com.example.jobstat.community.comment.entity.Comment, password: String?) {
        comment.password?.let { storedPassword ->
            // 비회원 댓글인 경우
            validateGuestPermission(password, storedPassword)
        } ?: run {
            // 회원 댓글인 경우
            validateMemberPermission(comment)
        }
    }

    private fun validateGuestPermission(password: String?, storedPassword: String) {
        password ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
        if (!passwordUtil.matches(password, storedPassword)) {
            throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
        }
    }

    private fun validateMemberPermission(comment: com.example.jobstat.community.comment.entity.Comment) {
        val currentUserId = securityUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            
        require(comment.userId == currentUserId) { 
            CommentConstants.ErrorMessages.UNAUTHORIZED_UPDATE 
        }
    }

    data class Request(
        @field:NotBlank
        @field:Size(
            min = CommentConstants.MIN_CONTENT_LENGTH,
            max = CommentConstants.MAX_CONTENT_LENGTH,
        )
        val content: String,
        
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        val password: String?,
    ) {
        fun of(commentId: Long): ExecuteRequest = ExecuteRequest(
            commentId = commentId,
            content = this.content,
            password = this.password,
        )
    }

    data class ExecuteRequest(
        @field:Positive
        val commentId: Long,
        
        @field:NotBlank
        @field:Size(
            min = CommentConstants.MIN_CONTENT_LENGTH,
            max = CommentConstants.MAX_CONTENT_LENGTH,
        )
        val content: String,
        
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
        )
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
