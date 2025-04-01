package com.example.jobstat.community_read.client.response

/**
 * 인기 게시글 응답 DTO
 */
data class TopBoardsResponseDto(
    val items: List<TopBoardResponse>
)

/**
 * 인기 게시글 아이템 DTO
 */
data class TopBoardResponse(
    val id: Long,
    val title: String,
    val author: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val categoryId: Long,
    val createdAt: String
) 