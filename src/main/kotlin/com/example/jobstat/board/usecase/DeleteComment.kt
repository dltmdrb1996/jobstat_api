package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

@Service
internal class DeleteComment(
    private val commentService: CommentService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<DeleteComment.Request, DeleteComment.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val comment = commentService.getCommentById(request.commentId)
        require(bcryptPasswordUtil.matches(request.password, comment.password!!)) { "비밀번호가 일치하지 않습니다" }
        commentService.deleteComment(request.commentId)
        return Response(success = true)
    }

    data class Request(
        @field:Positive val commentId: Long,
        @field:NotBlank val password: String,
    )

    data class Response(
        val success: Boolean,
    )
}
