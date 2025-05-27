package com.wildrew.jobstat.community.comment.usecase.command

import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.utils.CommentConstants
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.PasswordUtil
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteComment(
    private val commentService: CommentService,
    private val passwordUtil: PasswordUtil,
    private val theadContextUtils: TheadContextUtils,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<DeleteComment.ExecuteRequest, DeleteComment.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: ExecuteRequest): Response = super.invoke(request)

    override fun execute(request: ExecuteRequest): Response {
        // 댓글 조회 및 권한 검증
        val comment = commentService.getCommentById(request.commentId)

        log.debug("password: ${request.password}")
        log.debug("comment.password: ${comment.password}")
        // 접근 권한 검증
        validatePermission(comment, request.password)

        val boardId = comment.board.id

        commentService.deleteComment(request.commentId)

        // 이벤트 발행
        communityCommandEventPublisher.publishCommentDeleted(
            commentId = comment.id,
            boardId = boardId,
        )

        return Response(success = true)
    }

    private fun validatePermission(
        comment: Comment,
        password: String?,
    ) {
        comment.password?.let { storedPassword ->
            // 비회원 댓글인 경우
            validatePasswordAccess(comment, password)
        } ?: run {
            // 회원 댓글인 경우
            validateMemberAccess(comment)
        }
    }

    private fun validatePasswordAccess(
        comment: Comment,
        password: String?,
    ) {
        // 비밀번호 필수 체크
        val pwd =
            password ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 필요합니다",
            )

        // 비밀번호 일치 검증
        if (!passwordUtil.matches(pwd, comment.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다",
            )
        }

        log.debug("[DeleteComment] 비회원 댓글 ${comment.id}번이 비밀번호로 삭제되었습니다")
    }

    private fun validateMemberAccess(comment: Comment) {
        // 현재 사용자 ID 확인
        val currentUserId =
            theadContextUtils.getCurrentUserId()
                ?: throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    "로그인이 필요합니다",
                )

        when {
            // 관리자는 모든 댓글 삭제 가능
            theadContextUtils.isAdmin() -> {
                log.debug("[DeleteComment] 관리자 사용자 $currentUserId 댓글 ${comment.id}번을 삭제합니다")
            }
            // 일반 사용자는 자신의 댓글만 삭제 가능
            comment.userId != currentUserId -> {
                throw AppException.fromErrorCode(
                    ErrorCode.INSUFFICIENT_PERMISSION,
                    "본인의 댓글만 삭제할 수 있습니다",
                )
            }
        }
    }

    @Schema(
        name = "DeleteCommentRequest",
        description = "댓글 삭제 요청 모델",
    )
    data class Request(
        @Schema(
            description = "비밀번호 (비회원 댓글 삭제 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = CommentConstants.MIN_PASSWORD_LENGTH,
            maxLength = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${CommentConstants.MIN_PASSWORD_LENGTH}~${CommentConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다",
        )
        val password: String?,
    ) {
        fun of(commentId: Long): ExecuteRequest =
            ExecuteRequest(
                commentId = commentId,
                password = password,
            )
    }

    @Schema(
        name = "DeleteCommentExecuteRequest",
        description = "댓글 삭제 실행 요청 모델",
    )
    data class ExecuteRequest(
        @Schema(
            description = "삭제할 댓글 ID",
            example = "1",
            required = true,
        )
        val commentId: Long,
        @Schema(
            description = "비밀번호 (비회원 댓글 삭제 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = CommentConstants.MIN_PASSWORD_LENGTH,
            maxLength = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${CommentConstants.MIN_PASSWORD_LENGTH}~${CommentConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다",
        )
        val password: String?,
    )

    @Schema(
        name = "DeleteCommentResponse",
        description = "댓글 삭제 응답 모델",
    )
    data class Response(
        @Schema(
            description = "삭제 성공 여부",
            example = "true",
        )
        val success: Boolean,
    )
}
