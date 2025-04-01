package com.example.jobstat.community_read.model

import java.time.LocalDateTime


data class BoardReadModel(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val categoryId: Long? = null,
    val userId: Long? = null,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false
)

