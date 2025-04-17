package com.example.jobstat.community_read.model

import java.time.LocalDateTime

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
