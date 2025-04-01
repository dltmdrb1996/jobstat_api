package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.comment.CommentConstants
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 댓글 삭제 유스케이스
 * 로그인 사용자는 자신의 댓글만 삭제 가능
 * 비로그인 사용자는 비밀번호 검증 후 삭제 가능
 */
@Service
internal class DeleteComment(
    private val commentService: CommentService,
    private val passwordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<DeleteComment.ExecuteRequest, DeleteComment.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        // 댓글 조회 및 권한 검증
        commentService.getCommentById(request.commentId).also { comment ->
            validateDeletionPermission(comment, request.password)
        }

        // 댓글 삭제 및 로깅
        commentService.deleteComment(request.commentId)
        log.info("Comment id=${request.commentId} on board id=${request.boardId} deleted successfully")

        return Response(success = true)
    }

    private fun validateDeletionPermission(comment: Comment, password: String?) {
        comment.password?.let { storedPassword ->
            validateGuestPermission(comment, password)
        } ?: validateMemberPermission(comment)
    }

    private fun validateGuestPermission(comment: Comment, password: String?) {
        // 비밀번호 필수 체크
        val pwd = password ?: throw AppException.fromErrorCode(
            ErrorCode.AUTHENTICATION_FAILURE,
            "비밀번호가 필요합니다"
        )

        // 비밀번호 일치 검증
        if (!passwordUtil.matches(pwd, comment.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다"
            )
        }

        log.info("Guest comment id=${comment.id} deletion authorized with password")
    }

    private fun validateMemberPermission(comment: Comment) {
        // 현재 사용자 ID 확인
        val currentUserId = securityUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "로그인이 필요합니다"
            )

        // 관리자 권한 확인
        if (securityUtils.isAdmin()) {
            log.info("Admin user id=${currentUserId} deleting comment id=${comment.id}")
            return
        }

        // 본인 확인
        if (comment.userId != currentUserId) {
            throw AppException.fromErrorCode(
                ErrorCode.INSUFFICIENT_PERMISSION,
                CommentConstants.ErrorMessages.UNAUTHORIZED_DELETE
            )
        }
    }

    data class Request(
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        val password: String?,
    ) {
        fun of(
            boardId: Long,
            commentId: Long,
        ): ExecuteRequest = ExecuteRequest(
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

        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        val password: String?,
    )

    data class Response(
        val success: Boolean,
    )
}