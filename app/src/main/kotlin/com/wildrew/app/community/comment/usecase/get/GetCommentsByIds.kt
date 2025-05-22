package com.wildrew.app.community.comment.usecase.get

import com.wildrew.app.community.comment.entity.Comment
import com.wildrew.app.community.comment.service.CommentService
import com.wildrew.app.community.comment.utils.CommentMapperUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GetCommentsByIds(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentsByIds.Request, GetCommentsByIds.Response>(validator) {
    override fun execute(request: Request): Response {
        val comments = commentService.getCommentsByIds(request.commentIds)

        val commentItems =
            comments.map { comment ->
                mapToCommentItem(comment)
            }

        return Response(commentItems)
    }

    private fun mapToCommentItem(comment: Comment): CommentItem =
        CommentMapperUtils.mapToCommentDto(
            comment,
            { id, boardId, userId, author, content, createdAt, updatedAt, eventTs ->
                CommentItem(
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

    @Schema(
        name = "GetCommentsByIdsRequest",
        description = "여러 댓글 조회 요청 모델",
    )
    data class Request(
        @Schema(
            description = "조회할 댓글 ID 목록",
            example = "[1, 2, 3]",
            required = true,
        )
        @field:NotEmpty(message = "댓글 ID 목록은 비어있을 수 없습니다")
        val commentIds: List<Long>,
    )

    @Schema(
        name = "GetCommentsByIdsResponse",
        description = "여러 댓글 조회 응답 모델",
    )
    data class Response(
        @Schema(
            description = "댓글 목록",
        )
        val comments: List<CommentItem>,
    )

    @Schema(
        name = "CommentBulkResponseItem",
        description = "여러 댓글 조회 응답 아이템",
    )
    data class CommentItem(
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
    )
}
