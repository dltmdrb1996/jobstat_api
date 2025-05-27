package com.wildrew.jobstat.community_read.community_read.fixture

import com.wildrew.jobstat.community_read.model.CommentReadModel
import java.time.LocalDateTime

class CommentReadModelFixture private constructor(
    private var id: Long = 101L,
    private var boardId: Long = 1L,
    private var userId: Long? = 20L,
    private var author: String = "댓글작성자",
    private var content: String = "읽기 모델 댓글 내용입니다.",
    private var createdAt: LocalDateTime = LocalDateTime.now().minusHours(1),
    private var updatedAt: LocalDateTime? = null,
    private var eventTs: Long = System.currentTimeMillis() - 1000,
) {
    fun default(): CommentReadModel = CommentReadModelFixture().create()

    fun create(): CommentReadModel =
        CommentReadModel(
            id = id,
            boardId = boardId,
            userId = userId,
            author = author,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
            eventTs = eventTs,
        )

    fun withId(id: Long) = apply { this.id = id }

    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withUserId(userId: Long?) = apply { this.userId = userId }

    fun asGuest() = apply { this.userId = null }

    fun withAuthor(author: String) = apply { this.author = author }

    fun withContent(content: String) = apply { this.content = content }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: LocalDateTime?) = apply { this.updatedAt = updatedAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    companion object {
        fun aCommentReadModel() = CommentReadModelFixture()
    }
}
