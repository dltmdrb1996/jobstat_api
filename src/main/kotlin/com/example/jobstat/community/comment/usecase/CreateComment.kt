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

/**
 * 댓글 추가 유스케이스
 * 로그인 사용자는 자동으로 작성자 정보가 설정되고, 비로그인 사용자는 비밀번호 설정이 필요함
 */
@Service
internal class CreateComment(
    private val commentService: CommentService,
    private val securityUtils: SecurityUtils,
    private val passwordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<CreateComment.ExecuteRequest, CreateComment.Response>(validator) {

    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        val userId = securityUtils.getCurrentUserId()

        // 비로그인 상태에서 비밀번호 필수 검증
        validatePasswordRequirement(userId, request.password)

        // 비밀번호 인코딩 또는 null 처리
        val encodedPassword = processPassword(userId, request.password)

        // 댓글 생성 및 응답 반환
        return commentService.createComment(
            boardId = request.boardId,
            content = request.content,
            author = request.author,
            password = encodedPassword,
            userId = userId,
        ).let { comment ->
            Response(
                id = comment.id,
                content = comment.content,
                author = comment.author,
                boardId = comment.board.id,
                createdAt = comment.createdAt.toString()
            )
        }
    }

    private fun validatePasswordRequirement(userId: Long?, password: String?) {
        if (userId == null && password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                CommentConstants.ErrorMessages.PASSWORD_REQUIRED,
            )
        }
    }

    private fun processPassword(userId: Long?, password: String?): String? =
        userId?.let { null } ?: password?.let { passwordUtil.encode(it) }

    data class Request(
        @field:NotBlank
        @field:Size(
            min = CommentConstants.MIN_CONTENT_LENGTH,
            max = CommentConstants.MAX_CONTENT_LENGTH,
        )
        val content: String,

        @field:NotBlank
        val author: String,

        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        val password: String?,
    ) {
        fun of(boardId: Long): ExecuteRequest = ExecuteRequest(
            boardId = boardId,
            content = this.content,
            author = this.author,
            password = this.password,
        )
    }

    data class ExecuteRequest(
        @field:Positive
        val boardId: Long,

        @field:NotBlank
        @field:Size(
            min = CommentConstants.MIN_CONTENT_LENGTH,
            max = CommentConstants.MAX_CONTENT_LENGTH,
        )
        val content: String,

        @field:NotBlank
        val author: String,

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
        val boardId: Long,
        val createdAt: String,
    )
}