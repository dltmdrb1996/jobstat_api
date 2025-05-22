package com.wildrew.app.community.comment.usecase.command

import com.wildrew.app.community.comment.entity.Comment
import com.wildrew.app.community.comment.service.CommentService
import com.wildrew.app.community.comment.utils.CommentMapperUtils
import com.wildrew.app.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.PasswordUtil
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateComment(
    private val commentService: CommentService,
    private val passwordUtil: PasswordUtil,
    private val theadContextUtils: TheadContextUtils,
    private val eventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<UpdateComment.ExecuteRequest, UpdateComment.Response>(validator) {
    @Transactional
    override fun invoke(request: ExecuteRequest): Response = super.invoke(request)

    override fun execute(request: ExecuteRequest): Response {
        val comment = commentService.getCommentById(request.commentId)

        validateUpdatePermission(comment, request.password)

        val updated =
            commentService.updateComment(
                id = request.commentId,
                content = request.content,
            )

        eventPublisher.publishCommentUpdated(
            comment = updated,
            boardId = updated.board.id,
        )

        return createResponse(updated)
    }

    private fun createResponse(comment: Comment): Response =
        CommentMapperUtils.mapToCommentDtoWithStringDates(
            comment,
            { id, boardId, _, author, content, createdAt, updatedAt, _ ->
                Response(
                    id = id,
                    boardId = boardId,
                    content = content,
                    author = author,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            },
        )

    private fun validateUpdatePermission(
        comment: Comment,
        password: String?,
    ) {
        if (comment.password != null) {
            validateGuestPermission(comment, password)
        } else {
            validateMemberPermission(comment)
        }
    }

    private fun validateGuestPermission(
        comment: Comment,
        password: String?,
    ) {
        if (password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 필요합니다",
            )
        }

        if (!passwordUtil.matches(password, comment.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다",
            )
        }
    }

    private fun validateMemberPermission(comment: Comment) {
        val currentUserId =
            theadContextUtils.getCurrentUserId()
                ?: throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    "로그인이 필요합니다",
                )

        if (comment.userId != currentUserId && !theadContextUtils.isAdmin()) {
            throw AppException.fromErrorCode(
                ErrorCode.INSUFFICIENT_PERMISSION,
                "본인의 댓글만 수정할 수 있습니다",
            )
        }
    }

    @Schema(
        name = "UpdateCommentRequest",
        description = "댓글 수정 요청 모델",
    )
    data class Request(
        @Schema(
            description = "수정할 댓글 내용",
            example = "수정된 댓글 내용입니다",
            required = true,
            minLength = 1,
            maxLength = 1000,
        )
        val content: String,
        @Schema(
            description = "비밀번호 (비회원 댓글 수정 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = 4,
            maxLength = 15,
        )
        val password: String?,
    ) {
        fun of(commentId: Long) =
            ExecuteRequest(
                commentId = commentId,
                content = this.content,
                password = this.password,
            )
    }

    @Schema(
        name = "UpdateCommentExecuteRequest",
        description = "댓글 수정 실행 요청 모델",
    )
    data class ExecuteRequest(
        @Schema(
            description = "수정할 댓글 ID",
            example = "1",
            required = true,
        )
        val commentId: Long,
        @Schema(
            description = "수정할 댓글 내용",
            example = "수정된 댓글 내용입니다",
            required = true,
            minLength = 1,
            maxLength = 1000,
        )
        @field:NotBlank(message = "댓글 내용은 필수입니다")
        @field:Size(
            max = 1000,
            message = "댓글 내용은 최대 1000자까지 입력 가능합니다",
        )
        val content: String,
        @Schema(
            description = "비밀번호 (비회원 댓글 수정 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = 4,
            maxLength = 15,
        )
        @field:Size(
            min = 4,
            max = 15,
            message = "비밀번호는 4~15자 사이여야 합니다",
        )
        val password: String?,
    )

    @Schema(
        name = "UpdateCommentResponse",
        description = "댓글 수정 응답 모델",
    )
    data class Response(
        @Schema(
            description = "댓글 ID",
            example = "1",
        )
        val id: String,
        @Schema(
            description = "게시글 ID",
            example = "1",
        )
        val boardId: String,
        @Schema(
            description = "댓글 내용",
            example = "수정된 댓글 내용입니다",
        )
        val content: String,
        @Schema(
            description = "작성자",
            example = "홍길동",
        )
        val author: String,
        @Schema(
            description = "생성 시간",
            example = "2023-01-01T12:34:56",
        )
        val createdAt: String,
        @Schema(
            description = "수정 시간",
            example = "2023-01-02T12:34:56",
        )
        val updatedAt: String,
    )
}
