package com.example.jobstat.community_read.model

import java.time.LocalDateTime

/**
 * 댓글 읽기 모델
 * Redis에 저장될 댓글 데이터 구조
 */
data class CommentReadModel(
    val id: Long,
    val boardId: Long,
    val userId: Long? = null,
    val author: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null,
    val eventTs: Long,
)
