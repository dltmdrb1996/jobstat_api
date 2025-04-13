package com.example.jobstat.community.comment.usecase.command

import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.comment.utils.CommentConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.transaction.annotation.Transactional
import jakarta.validation.Validator
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
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<DeleteComment.ExecuteRequest, DeleteComment.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: ExecuteRequest): Response {
        return super.invoke(request)
    }

    override fun execute(request: ExecuteRequest): Response {
        // 댓글 조회 및 권한 검증
        val comment = commentService.getCommentById(request.commentId)
        
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

    private fun validatePermission(comment: Comment, password: String?) {
        comment.password?.let { storedPassword -> 
            // 비회원 댓글인 경우
            validatePasswordAccess(comment, password)
        } ?: run {
            // 회원 댓글인 경우
            validateMemberAccess(comment)
        }
    }

    private fun validatePasswordAccess(comment: Comment, password: String?) {
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

        log.info("[DeleteComment] Anonymous comment ${comment.id} deleted with password")
    }

    private fun validateMemberAccess(comment: Comment) {
        // 현재 사용자 ID 확인
        val currentUserId = securityUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "로그인이 필요합니다"
            )

        when {
            // 관리자는 모든 댓글 삭제 가능
            securityUtils.isAdmin() -> {
                log.info("[DeleteComment] Admin user $currentUserId deleting comment ${comment.id}")
            }
            // 일반 사용자는 자신의 댓글만 삭제 가능
            comment.userId != currentUserId -> {
                throw AppException.fromErrorCode(
                    ErrorCode.INSUFFICIENT_PERMISSION,
                    "본인의 댓글만 삭제할 수 있습니다"
                )
            }
        }
    }

    @Schema(
        name = "DeleteCommentRequest",
        description = "댓글 삭제 요청 모델"
    )
    data class Request(
        @Schema(
            description = "비밀번호 (비회원 댓글 삭제 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = CommentConstants.MIN_PASSWORD_LENGTH,
            maxLength = CommentConstants.MAX_PASSWORD_LENGTH
        )
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${CommentConstants.MIN_PASSWORD_LENGTH}~${CommentConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다"
        )
        val password: String?,
    ) {
        fun of(commentId: Long): ExecuteRequest = ExecuteRequest(
            commentId = commentId,
            password = password,
        )
    }

    @Schema(
        name = "DeleteCommentExecuteRequest",
        description = "댓글 삭제 실행 요청 모델"
    )
    data class ExecuteRequest(
        @Schema(
            description = "삭제할 댓글 ID",
            example = "1",
            required = true
        )
        val commentId: Long,

        @Schema(
            description = "비밀번호 (비회원 댓글 삭제 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = CommentConstants.MIN_PASSWORD_LENGTH,
            maxLength = CommentConstants.MAX_PASSWORD_LENGTH
        )
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${CommentConstants.MIN_PASSWORD_LENGTH}~${CommentConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다"
        )
        val password: String?,
    )

    @Schema(
        name = "DeleteCommentResponse",
        description = "댓글 삭제 응답 모델"
    )
    data class Response(
        @Schema(
            description = "삭제 성공 여부",
            example = "true"
        )
        val success: Boolean,
    )
} 