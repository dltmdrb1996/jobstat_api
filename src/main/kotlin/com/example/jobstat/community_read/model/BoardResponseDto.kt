package com.example.jobstat.community_read.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "게시글 응답 DTO")
data class BoardResponseDto(
    @field:Schema(description = "게시글 ID", example = "65f1a8f3b4a9d81b9b0a8e1a") // 예시 ID 변경
    val id: String,

    @field:Schema(description = "카테고리 ID", example = "2")
    val categoryId: String,

    @field:Schema(description = "게시글 제목", example = "안녕하세요, 첫 게시글입니다")
    val title: String,

    @field:Schema(description = "게시글 내용", example = "게시글 내용입니다. 반갑습니다!")
    val content: String,

    @field:Schema(description = "작성자", example = "홍길동")
    val author: String,

    @field:Schema(description = "작성자 ID (Optional)", example = "123")
    val userId: String?,

    @field:Schema(description = "조회수", example = "42", minimum = "0")
    val viewCount: Int,

    @field:Schema(description = "좋아요 수", example = "15", minimum = "0")
    val likeCount: Int,

    @field:Schema(description = "댓글 수", example = "7", minimum = "0")
    val commentCount: Int,

    @field:Schema(description = "생성 일시", example = "2023-05-10T14:30:15.123456", format = "date-time")
    val createdAt: LocalDateTime,

    @field:Schema(description = "이벤트 타임스탬프 (밀리초)", example = "1683727815123")
    val eventTs: Long
) {
    companion object {
        fun from(board: BoardReadModel): BoardResponseDto {
            return BoardResponseDto(
                id = board.id.toString(), // BoardReadModel의 ID는 이미 String
                categoryId = board.categoryId.toString(), // Long -> String 변환
                title = board.title,
                content = board.content,
                author = board.author,
                userId = board.userId?.toString(), // Long? -> String? 변환
                viewCount = board.viewCount,
                likeCount = board.likeCount,
                commentCount = board.commentCount,
                createdAt = board.createdAt,
                eventTs = board.eventTs
            )
        }

        fun from(boards: List<BoardReadModel>?): List<BoardResponseDto> {
            return boards?.map { from(it) } ?: emptyList()
        }
    }
}
