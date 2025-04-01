package com.example.jobstat.community_read.client.response

import java.time.LocalDateTime

/**
 * 댓글 읽기 모델 응답 DTO
 */
data class CommentReadResponse(
    val id: Long,
    val boardId: Long,
    val content: String,
    val author: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val parentId: Long? = null,
    val likeCount: Int = 0
) 