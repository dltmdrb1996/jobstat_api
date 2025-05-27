package com.wildrew.jobstat.community.comment.usecase.get

import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.utils.CommentMapperUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

@Service
class GetCommentsByBoardIdAfter(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentsByBoardIdAfter.Request, GetCommentsByBoardIdAfter.Response>(validator) {
    override fun execute(request: Request): Response {
        val comments =
            commentService.getCommentsByBoardIdAfter(
                boardId = request.boardId,
                lastCommentId = request.lastCommentId,
                limit = request.limit ?: 20,
            )

        val items =
            comments.map { comment ->
                mapToCommentItem(comment)
            }

        return Response(
            items = items,
            hasNext = items.size >= (request.limit ?: 20),
        )
    }

    /**
     * 댓글 엔티티를 응답 모델로 변환
     */
    private fun mapToCommentItem(comment: Comment): CommentItem =
        com.wildrew.jobstat.community.comment.utils.CommentMapperUtils.mapToCommentDtoWithStringDates(
            comment,
            { id, boardId, userId, author, content, createdAt, updatedAt, eventTs ->
                CommentItem(
                    id = id,
                    boardId = boardId,
                    userId = userId,
                    author = author,
                    content = content,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    eventTs = eventTs,
                )
            },
        )

    @Schema(
        name = "GetCommentsByBoardIdAfterRequest",
        description = "게시글별 커서 기반 댓글 목록 조회 요청 모델",
    )
    data class Request(
        @Schema(
            description = "게시글 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "게시글 ID는 양수여야 합니다")
        val boardId: Long,
        @Schema(
            description = "마지막으로 조회한 댓글 ID",
            example = "15",
            nullable = true,
        )
        val lastCommentId: Long?,
        @Schema(
            description = "조회할 댓글 수",
            example = "20",
            nullable = true,
            defaultValue = "20",
        )
        val limit: Int?,
    )

    @Schema(
        name = "GetCommentsByBoardIdAfterResponse",
        description = "게시글별 커서 기반 댓글 목록 조회 응답 모델",
    )
    data class Response(
        @Schema(
            description = "댓글 목록",
        )
        val items: List<CommentItem>,
        @Schema(
            description = "다음 페이지 존재 여부",
            example = "true",
        )
        val hasNext: Boolean,
    )

    @Schema(
        name = "BoardCommentCursorItem",
        description = "게시글별 커서 기반 댓글 목록 아이템",
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
            description = "작성자 이름",
            example = "홍길동",
        )
        val author: String,
        @Schema(
            description = "댓글 내용",
            example = "좋은 글 감사합니다!",
        )
        val content: String,
        @Schema(
            description = "생성 시간",
            example = "2023-01-01T12:34:56",
        )
        val createdAt: String,
        @Schema(
            description = "수정 시간",
            example = "2023-01-01T12:34:56",
        )
        val updatedAt: String,
        @Schema(
            description = "이벤트 타임스탬프 (밀리초)",
            example = "1672531200000",
        )
        val eventTs: Long,
    )
}
