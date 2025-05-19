package com.example.jobstat.community.comment.usecase.command

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.comment.utils.CommentConstants
import com.example.jobstat.community.comment.utils.CommentMapperUtils
import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_security.util.SecurityUtils
import com.example.jobstat.core.core_security.util.PasswordUtil
import com.example.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class CreateComment(
    private val commentService: CommentService,
    private val securityUtils: SecurityUtils,
    private val passwordUtil: PasswordUtil,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<CreateComment.ExecuteRequest, CreateComment.Response>(validator) {
    @Transactional
    override fun invoke(request: ExecuteRequest): Response = super.invoke(request)

    override fun execute(request: ExecuteRequest): Response {
        val userId = securityUtils.getCurrentUserId()
        validatePasswordIfNotLoggedIn(userId, request.password)
        val encodedPassword = processPassword(userId, request.password)

        val createdComment =
            commentService.createComment(
                boardId = request.boardId,
                content = request.content,
                author = request.author,
                password = encodedPassword,
                userId = userId,
            )

        communityCommandEventPublisher.publishCommentCreated(
            comment = createdComment,
            boardId = request.boardId,
            userId = userId,
        )

        return Response.from(createdComment)
    }

    private fun validatePasswordIfNotLoggedIn(
        userId: Long?,
        password: String?,
    ) {
        if (userId == null && password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비로그인 상태에서는 비밀번호 설정이 필수입니다",
            )
        }
    }

    private fun processPassword(
        userId: Long?,
        password: String?,
    ): String? = userId?.let { null } ?: password?.let { passwordUtil.encode(it) }

    @Schema(
        name = "CreateCommentRequest",
        description = "댓글 생성 요청 본문 모델 (컨트롤러 @RequestBody 용)",
    )
    data class Request(
        @Schema(
            description = "댓글 내용",
            example = "좋은 글 감사합니다!",
            minLength = CommentConstants.MIN_CONTENT_LENGTH,
            maxLength = CommentConstants.MAX_CONTENT_LENGTH,
            required = true,
        )
        val content: String,
        @Schema(
            description = "작성자 이름",
            example = "홍길동",
            required = true,
        )
        val author: String,
        @Schema(
            description = "비밀번호 (비로그인 사용자만 필요)",
            example = "password1234",
            nullable = true,
            minLength = CommentConstants.MIN_PASSWORD_LENGTH,
            maxLength = CommentConstants.MAX_PASSWORD_LENGTH,
        )
        val password: String?,
    ) {
        /**
         * 컨트롤러에서 경로 파라미터 boardId와 본문 Request를 합쳐 ExecuteRequest를 만드는 함수
         */
        fun of(boardId: Long): ExecuteRequest =
            ExecuteRequest(
                boardId = boardId,
                content = this.content,
                author = this.author,
                password = this.password,
            )
    }

    @Schema(
        name = "CreateCommentResponse",
        description = "댓글 생성 응답 모델",
    )
    data class Response(
        @Schema(description = "댓글 ID", example = "1")
        val id: String,
        @Schema(description = "게시글 ID", example = "1")
        val boardId: String,
        @Schema(description = "작성자", example = "홍길동")
        val author: String,
        @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!")
        val content: String,
        @Schema(description = "생성 시간", example = "2023-01-01T12:34:56")
        val createdAt: String,
    ) {
        companion object {
            fun from(comment: Comment): Response =
                CommentMapperUtils.mapToCommentDtoWithStringDates(
                    comment,
                    { id, boardId, _, author, content, createdAt, _, _ ->
                        Response(
                            id = id,
                            boardId = boardId,
                            author = author,
                            content = content,
                            createdAt = createdAt,
                        )
                    },
                )
        }
    }

    data class ExecuteRequest(
        val boardId: Long,
        @field:NotBlank(message = "댓글 내용은 공백만으로 이루어질 수 없습니다")
        @field:Size(
            min = CommentConstants.MIN_CONTENT_LENGTH,
            max = CommentConstants.MAX_CONTENT_LENGTH,
            message = "댓글 내용은 ${CommentConstants.MIN_CONTENT_LENGTH}~${CommentConstants.MAX_CONTENT_LENGTH}자 사이여야 합니다",
        )
        val content: String,
        @field:NotBlank(message = "작성자 이름은 필수입니다")
        val author: String,
        @field:Size(
            min = CommentConstants.MIN_PASSWORD_LENGTH,
            max = CommentConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${CommentConstants.MIN_PASSWORD_LENGTH}~${CommentConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다",
        )
        val password: String?,
    )
}
