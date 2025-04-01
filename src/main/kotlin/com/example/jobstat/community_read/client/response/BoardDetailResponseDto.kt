package com.example.jobstat.community_read.client.response

/**
 * 게시글 상세 응답 DTO
 */
data class BoardDetailResponseDto(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val categoryId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val userLiked: Boolean,
    val createdAt: String,
    val comments: CommentListResponseDto? = null,
    val commentTotalCount: Long = 0,
    val commentHasNext: Boolean = false
) 