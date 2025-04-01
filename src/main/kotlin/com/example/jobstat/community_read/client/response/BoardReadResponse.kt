package com.example.jobstat.community_read.client.response

import java.time.LocalDateTime

/**
 * 게시글 읽기 모델 응답 DTO
 */
data class BoardReadResponse(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val categoryId: Long,
    val categoryName: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 