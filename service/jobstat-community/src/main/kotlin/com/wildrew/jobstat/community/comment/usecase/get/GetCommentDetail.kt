package com.wildrew.jobstat.community.comment.usecase.get

import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.utils.CommentMapperUtils
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GetCommentDetail(
    private val commentService: CommentService,
    private val theadContextUtils: TheadContextUtils,
    validator: Validator,
) : ValidUseCase<com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Request, _root_ide_package_.com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Response>(validator) {
    override fun execute(request: _root_ide_package_.com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Request): _root_ide_package_.com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Response {
        val comment = commentService.getCommentById(request.commentId)
        val userId = theadContextUtils.getCurrentUserIdOrNull()

        return _root_ide_package_.com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Response.Companion.from(
            comment,
        )
    }

    @Schema(
        name = "GetCommentDetailRequest",
        description = "댓글 상세 조회 요청 모델",
    )
    data class Request(
        @Schema(
            description = "조회할 댓글 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "댓글 ID는 양수여야 합니다")
        val commentId: Long,
    )

    @Schema(
        name = "GetCommentDetailResponse",
        description = "댓글 상세 조회 응답 모델",
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
            description = "작성자 ID (비회원인 경우 null)",
            example = "1002",
            nullable = true,
        )
        val userId: Long?,
        @Schema(
            description = "댓글 내용",
            example = "좋은 글 감사합니다!",
        )
        val content: String,
        @Schema(
            description = "작성자 이름",
            example = "홍길동",
        )
        val author: String,
        @Schema(
            description = "생성 시간",
            example = "2023-01-01T12:34:56",
            format = "date-time",
        )
        val createdAt: LocalDateTime,
        @Schema(
            description = "수정 시간",
            example = "2023-01-01T12:34:56",
            format = "date-time",
        )
        val updatedAt: LocalDateTime,
        @Schema(
            description = "이벤트 타임스탬프 (밀리초)",
            example = "1672531200000",
        )
        val eventTs: Long,
    ) {
        companion object {
            fun from(comment: Comment): _root_ide_package_.com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Response =
                com.wildrew.jobstat.community.comment.utils.CommentMapperUtils.mapToCommentDto(
                    comment,
                    { id, boardId, userId, author, content, createdAt, updatedAt, eventTs ->
                        _root_ide_package_.com.wildrew.jobstat.community.comment.usecase.get.GetCommentDetail.Response(
                            id = id,
                            boardId = boardId,
                            userId = userId,
                            content = content,
                            author = author,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            eventTs = eventTs,
                        )
                    },
                )
        }
    }
}
