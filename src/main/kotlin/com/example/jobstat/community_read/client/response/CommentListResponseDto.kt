package com.example.jobstat.community_read.client.response

/**
 * 댓글 목록 응답 DTO
 */
data class CommentListResponseDto(
    val items: List<CommentListItemDto>,
    val totalCount: Long,
    val hasNext: Boolean
)

/**
 * 댓글 목록 아이템 DTO
 */
data class CommentListItemDto(
    val id: Long,
    val content: String,
    val author: String,
    val createdAt: String
) 