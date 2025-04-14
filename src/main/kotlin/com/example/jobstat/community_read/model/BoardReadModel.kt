package com.example.jobstat.community_read.model

import java.time.LocalDateTime

/**
 * 게시글 읽기 모델
 * Redis에 저장될 게시글 데이터 구조
 */
data class BoardReadModel(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val author: String,
    val userId: Long?,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime,
    val eventTs: Long,
    var comments: List<CommentReadModel> = emptyList(),
)
