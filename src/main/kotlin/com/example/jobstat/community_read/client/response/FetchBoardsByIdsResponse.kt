package com.example.jobstat.community_read.client.response

import com.example.jobstat.community_read.model.BoardReadModel
import java.time.LocalDateTime

data class FetchBoardsByIdsResponse(
    val boards: List<BoardItem>
) {
    fun toBoardReadModels(): List<BoardReadModel> {
        return boards.map { it.toBoardReadModel() }
    }
}

data class BoardItem(
    val id: Long,
    val userId: Long?,
    val categoryId: Long,
    val title: String,
    val content: String,
    val author: String,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val userLiked: Boolean,
    val createdAt: LocalDateTime,
    val eventTs : Long
) {
    fun toBoardReadModel(): BoardReadModel {
        return BoardReadModel(
            id = id,
            createdAt = createdAt,
            title = title,
            content = content,
            author = author,
            categoryId = categoryId,
            userId = userId,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = commentCount,
            eventTs = eventTs
        )
    }
}



