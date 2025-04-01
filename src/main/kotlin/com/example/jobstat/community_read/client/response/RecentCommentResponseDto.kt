package com.example.jobstat.community_read.client.response

/**
 * 최근 댓글 응답 DTO
 */
data class RecentCommentResponseDto(
    val id: Long,
    val content: String,
    val author: String,
    val createdAt: String
) 