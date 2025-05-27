package com.wildrew.jobstat.community_read.community_read.fixture

import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.community_read.model.CommentReadModel
import java.time.LocalDateTime

class BoardReadModelFixture private constructor(
    private var id: Long = 1L,
    private var categoryId: Long = 1L,
    private var title: String = "읽기 모델 테스트 제목",
    private var content: String = "읽기 모델 테스트 내용입니다.",
    private var author: String = "읽기모델작성자",
    private var userId: Long? = 10L,
    private var viewCount: Int = 10,
    private var likeCount: Int = 5,
    private var commentCount: Int = 2,
    private var createdAt: LocalDateTime = LocalDateTime.now().minusDays(1),
    private var eventTs: Long = System.currentTimeMillis(),
    private var comments: List<CommentReadModel> = emptyList(),
) {
    fun create(): BoardReadModel =
        BoardReadModel(
            id = id,
            categoryId = categoryId,
            title = title,
            content = content,
            author = author,
            userId = userId,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = commentCount,
            createdAt = createdAt,
            eventTs = eventTs,
            comments = comments,
        )

    fun withId(id: Long) = apply { this.id = id }

    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }

    fun withTitle(title: String) = apply { this.title = title }

    fun withContent(content: String) = apply { this.content = content }

    fun withAuthor(author: String) = apply { this.author = author }

    fun withUserId(userId: Long?) = apply { this.userId = userId }

    fun asGuest() = apply { this.userId = null }

    fun withViewCount(viewCount: Int) = apply { this.viewCount = viewCount }

    fun withLikeCount(likeCount: Int) = apply { this.likeCount = likeCount }

    fun withCommentCount(commentCount: Int) = apply { this.commentCount = commentCount }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withComments(comments: List<CommentReadModel>) = apply { this.comments = comments }

    companion object {
        fun aBoardReadModel() = BoardReadModelFixture()
    }
}
