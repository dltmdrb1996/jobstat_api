package com.example.jobstat.community_read.client.response

import java.time.LocalDateTime

/**
 * 댓글 응답 DTO
 */
data class CommentResponseDto(
    val id: Long,
    val boardId: Long,
    val content: String,
    val author: String,
    val parentCommentId: Long?,
    val deleted: Boolean,
    val likeCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 