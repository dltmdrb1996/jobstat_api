package com.wildrew.app.community_read.client.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

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