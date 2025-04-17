package com.example.jobstat.community_read.model

import java.time.LocalDateTime

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
