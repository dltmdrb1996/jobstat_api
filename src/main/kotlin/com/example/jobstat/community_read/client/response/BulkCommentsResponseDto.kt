package com.example.jobstat.community_read.client.response

/**
 * 댓글 벌크 조회 응답 DTO
 */
data class BulkCommentsResponseDto(
    val comments: List<CommentItem>
) {
    data class CommentItem(
        val id: Long,
        val boardId: Long,
        val content: String,
        val author: String,
        val createdAt: String,
        val updatedAt: String
    )
} 