package com.wildrew.jobstat.community_read.client.response

import com.wildrew.jobstat.community_read.model.BoardReadModel // BoardReadModel import 가정
import java.time.LocalDateTime

data class FetchBoardsByIdsResponse(
    val boards: List<BoardItem>,
) {
    companion object {
        fun from(response: FetchBoardsByIdsResponse): List<BoardReadModel> = response.boards.map { BoardItem.from(it) }
    }
}

data class BoardItem(
    val id: String,
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
    val eventTs: Long,
) {
    companion object {
        fun from(item: BoardItem): BoardReadModel =
            BoardReadModel(
                id = item.id.toLong(),
                createdAt = item.createdAt,
                title = item.title,
                content = item.content,
                author = item.author,
                categoryId = item.categoryId,
                userId = item.userId,
                viewCount = item.viewCount,
                likeCount = item.likeCount,
                commentCount = item.commentCount,
                eventTs = item.eventTs,
            )
    }
}
