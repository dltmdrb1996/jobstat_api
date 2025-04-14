package com.example.jobstat.community_read.client.response

import com.example.jobstat.community_read.model.BoardReadModel // BoardReadModel import 가정
import java.time.LocalDateTime

data class FetchBoardByIdResponse(
    // id 타입을 String으로 변경
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
        fun from(response: FetchBoardByIdResponse): BoardReadModel =
            BoardReadModel(
                // BoardReadModel의 id가 String이라고 가정하고 할당
                id = response.id.toLong(),
                createdAt = response.createdAt,
                title = response.title,
                content = response.content,
                author = response.author,
                categoryId = response.categoryId,
                viewCount = response.viewCount,
                likeCount = response.likeCount,
                commentCount = response.commentCount,
                userId = response.userId,
                eventTs = response.eventTs,
                // BoardReadModel에 userLiked 필드가 있다면 추가
                // userLiked = response.userLiked
            )
    }
}
