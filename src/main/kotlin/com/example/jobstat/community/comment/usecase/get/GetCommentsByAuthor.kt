package com.example.jobstat.community.comment.usecase.get

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.comment.utils.CommentConstants
import com.example.jobstat.community.comment.utils.CommentMapperUtils
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 작성자별 댓글 목록 조회 유스케이스
 */
@Service
internal class GetCommentsByAuthor(
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetCommentsByAuthor.Request, GetCommentsByAuthor.Response>(validator) {
    override fun execute(request: Request): Response =
        PageRequest.of(request.page ?: 0, CommentConstants.DEFAULT_PAGE_SIZE).let { pageRequest ->
            commentService
                .getCommentsByAuthor(request.author, pageRequest)
                .map { mapToCommentListItem(it) }
                .let { commentsPage ->
                    Response(
                        items = commentsPage,
                        totalCount = commentsPage.totalElements,
                        hasNext = commentsPage.hasNext(),
                    )
                }
        }

    /**
     * 댓글 엔티티를 응답 모델로 변환
     */
    private fun mapToCommentListItem(comment: Comment): CommentListItem =
        CommentMapperUtils.mapToCommentDto(
            comment,
            { id, boardId, userId, author, content, createdAt, updatedAt, eventTs ->
                CommentListItem(
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
        name = "GetCommentsByAuthorRequest",
        description = "작성자별 댓글 목록 조회 요청 모델",
    )
    data class Request(
        @Schema(
            description = "조회할 작성자 이름",
            example = "홍길동",
            required = true,
        )
        @field:NotBlank(message = "작성자 이름은 필수입니다")
        val author: String,
        @Schema(
            description = "페이지 번호 (0부터 시작)",
            example = "0",
            nullable = true,
            defaultValue = "0",
        )
        val page: Int?,
    )

    @Schema(
        name = "GetCommentsByAuthorResponse",
        description = "작성자별 댓글 목록 조회 응답 모델",
    )
    data class Response(
        @Schema(
            description = "댓글 목록",
        )
        val items: Page<CommentListItem>,
        @Schema(
            description = "전체 댓글 수",
            example = "42",
        )
        val totalCount: Long,
        @Schema(
            description = "다음 페이지 존재 여부",
            example = "true",
        )
        val hasNext: Boolean,
    )

    @Schema(
        name = "CommentListItem",
        description = "댓글 목록 아이템",
    )
    data class CommentListItem(
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
