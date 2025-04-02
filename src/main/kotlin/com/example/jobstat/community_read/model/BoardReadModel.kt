package com.example.jobstat.community_read.model

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import java.time.LocalDateTime


data class BoardReadModel(
    val id: Long,
    val createdAt: LocalDateTime,
    val eventTs : Long,
    val title: String,
    val content: String,
    val author: String,
    val categoryId: Long,
    val userId: Long?,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
) {
    companion object {
        fun fromMap(map: Map<String, String>) : BoardReadModel {
            val createdAt = map["createdAt"] ?: throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED)
            return BoardReadModel(
                id = map["id"]?.toLong() ?: 0L,
                createdAt = LocalDateTime.parse(createdAt),
                eventTs = map["eventTs"]?.toLong() ?: 0L,
                title = map["title"] ?: "",
                content = map["content"] ?: "",
                author = map["author"] ?: "",
                categoryId = map["categoryId"]?.toLong() ?: 0L,
                userId = map["writerId"]?.toLongOrNull(),
                viewCount = map["viewCount"]?.toIntOrNull() ?: 0,
                likeCount = map["likeCount"]?.toIntOrNull() ?: 0,
                commentCount = map["commentCount"]?.toIntOrNull() ?: 0
            )
        }
    }
}