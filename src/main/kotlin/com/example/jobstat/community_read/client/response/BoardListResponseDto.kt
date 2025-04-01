package com.example.jobstat.community_read.client.response

/**
 * 게시글 목록 응답 DTO
 */
data class BoardListResponseDto(
    val items: List<BoardListItemDto>,
    val totalCount: Long,
    val hasNext: Boolean
)

/**
 * 게시글 목록 아이템 DTO
 */
data class BoardListItemDto(
    val id: Long,
    val title: String,
    val author: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val categoryId: Long,
    val createdAt: String
) 