package com.example.jobstat.community.comment.usecase.get

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.global.extension.toEpochMilli
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 댓글 상세 조회 유스케이스
 */
@Service
internal class GetCommentDetail(
    private val commentService: CommentService,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<GetCommentDetail.Request, GetCommentDetail.Response>(validator) {

    override fun execute(request: Request): Response {
        val comment = commentService.getCommentById(request.commentId)
        val userId = securityUtils.getCurrentUserId()
        
        return Response.from(comment)
    }

    @Schema(
        name = "GetCommentDetailRequest",
        description = "댓글 상세 조회 요청 모델"
    )
    data class Request(
        @Schema(
            description = "조회할 댓글 ID",
            example = "1",
            required = true
        )
        @field:Positive(message = "댓글 ID는 양수여야 합니다")
        val commentId: Long,
    )

    @Schema(
        name = "GetCommentDetailResponse",
        description = "댓글 상세 조회 응답 모델"
    )
    data class Response(
        @Schema(
            description = "댓글 ID",
            example = "1"
        )
        val id: String,
        
        @Schema(
            description = "게시글 ID",
            example = "1"
        )
        val boardId: String,
        
        @Schema(
            description = "작성자 ID (비회원인 경우 null)",
            example = "1002",
            nullable = true
        )
        val userId: Long?,
        
        @Schema(
            description = "댓글 내용",
            example = "좋은 글 감사합니다!"
        )
        val content: String,
        
        @Schema(
            description = "작성자 이름",
            example = "홍길동"
        )
        val author: String,
        
        @Schema(
            description = "생성 시간",
            example = "2023-01-01T12:34:56",
            format = "date-time"
        )
        val createdAt: LocalDateTime,
        
        @Schema(
            description = "수정 시간",
            example = "2023-01-01T12:34:56",
            format = "date-time"
        )
        val updatedAt: LocalDateTime,
        
        @Schema(
            description = "이벤트 타임스탬프 (밀리초)",
            example = "1672531200000"
        )
        val eventTs: Long,
    ) {
        companion object {
            fun from(comment: Comment): Response = with(comment) {
                Response(
                    id = id.toString(),
                    boardId = board.id.toString(),
                    userId = userId,
                    content = content,
                    author = author,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    eventTs = updatedAt.toEpochMilli(),
                )
            }
        }
    }
} 