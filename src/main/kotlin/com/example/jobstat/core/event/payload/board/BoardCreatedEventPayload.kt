package com.example.jobstat.core.event.payload.board

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.core.event.EventPayload
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 게시글 생성 이벤트 페이로드
 */
data class BoardCreatedEventPayload(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("userId")
    val userId: Long? = null,
    @JsonProperty("categoryId")
    val categoryId: Long,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("eventTs")
    val eventTs: Long,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("author")
    val author: String,
) : EventPayload {
    fun toReadModel() =
        BoardReadModel(
            id = boardId,
            userId = userId,
            categoryId = categoryId,
            createdAt = createdAt,
            title = title,
            content = content,
            author = author,
            commentCount = 0,
            likeCount = 0,
            viewCount = 0,
            eventTs = eventTs,
        )
}
