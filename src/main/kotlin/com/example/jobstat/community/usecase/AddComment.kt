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
internal class AddComment(
    private val commentService: CommentService,
    private val securityUtils: SecurityUtils,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<AddComment.ExecuteRequest, AddComment.Response>(validator) {
    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        val userId = securityUtils.getCurrentUserId()

        if (userId == null && request.password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비로그인 상태에서는 비밀번호 설정이 필수입니다",
            )
        }

        val password =
            if (userId == null) {
                request.password?.let { bcryptPasswordUtil.encode(it) }
            } else {
                null
            }

        val comment =
            commentService.createComment(
                boardId = request.boardId,
                content = request.content,
                author = request.author,
                password = password,
                userId = userId,
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
        @field:NotBlank
        @field:Size(max = 1000)
        val content: String,
        @field:NotBlank
        val author: String,
        @field:Size(min = 4, max = 10)
        val password: String?,
    ) {
        fun of(boardId: Long) =
            ExecuteRequest(
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
        @field:Size(max = 1000)
        val content: String,
        @field:NotBlank
        val author: String,
        @field:Size(min = 4, max = 10)
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
