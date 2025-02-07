package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
class UpdateComment(
    private val commentService: CommentService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<UpdateComment.Request, UpdateComment.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val comment = commentService.getCommentById(request.commentId)
        require(bcryptPasswordUtil.matches(request.password, comment.password!!)) { "비밀번호가 일치하지 않습니다" }
        val updated = commentService.updateComment(request.commentId, request.content)
        return Response(
            id = updated.id,
            content = updated.content,
            createdAt = updated.createdAt.toString(),
            updatedAt = updated.updatedAt.toString(),
        )
    }

    data class Request(
        @field:Positive val commentId: Long,
        @field:NotBlank @field:Size(max = 1000) val content: String,
        @field:NotBlank val password: String,
    )

    data class Response(
        val id: Long,
        val content: String,
        val createdAt: String,
        val updatedAt: String,
    )
}
