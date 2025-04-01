package com.example.jobstat.community_read.client.response

/**
 * 게시글 벌크 조회 응답 DTO
 */
data class BulkBoardsResponseDto(
    val boards: List<BoardItem>
) {
    data class BoardItem(
        val id: Long,
        val title: String,
        val content: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val categoryId: Long,
        val createdAt: String
    )
} 