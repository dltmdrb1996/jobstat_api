package com.wildrew.app.community_read.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "댓글 응답 DTO")
data class CommentResponseDto(
    @field:Schema(description = "댓글 ID", example = "101")
    val id: String,
    @field:Schema(description = "게시글 ID", example = "65f1a8f3b4a9d81b9b0a8e1a")
    val boardId: String,
    @field:Schema(description = "작성자 ID (Optional)", example = "124")
    val userId: String?,
    @field:Schema(description = "작성자", example = "김댓글")
    val author: String,
    @field:Schema(description = "댓글 내용", example = "댓글 내용입니다.")
    val content: String,
    @field:Schema(description = "생성 일시", example = "2023-05-10T15:00:00", format = "date-time")
    val createdAt: LocalDateTime,
    @field:Schema(description = "수정 일시 (Optional)", example = "2023-05-10T15:30:00", format = "date-time")
    val updatedAt: LocalDateTime?,
    @field:Schema(description = "이벤트 타임스탬프 (밀리초)", example = "1683728000000")
    val eventTs: Long,
) {
    companion object {
        fun from(comment: CommentReadModel): CommentResponseDto =
            CommentResponseDto(
                id = comment.id.toString(),
                boardId = comment.boardId.toString(),
                userId = comment.userId?.toString(),
                author = comment.author,
                content = comment.content,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                eventTs = comment.eventTs,
            )

        fun from(comments: List<CommentReadModel>?): List<CommentResponseDto> = comments?.map { from(it) } ?: emptyList()
    }
}
