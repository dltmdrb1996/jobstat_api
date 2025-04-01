package com.example.jobstat.community_read.model

import java.time.LocalDateTime

/**
 * 댓글 읽기 모델
 * Redis에 저장될 댓글 데이터 구조
 */
data class CommentReadModel(
    val id: Long,
    val boardId: Long,
    val content: String,
    val author: String,
    val userId: Long? = null,
    val likeCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false
) 