package com.wildrew.jobstat.community_read.payload

import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import java.time.LocalDateTime

class BoardCreatedEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var userId: Long? = 10L,
    private var categoryId: Long = 1L,
    private var createdAt: LocalDateTime = LocalDateTime.now().minusDays(1),
    private var eventTs: Long = System.currentTimeMillis(),
    private var title: String = "페이로드 제목",
    private var content: String = "페이로드 내용",
    private var author: String = "페이로드 작성자",
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withUserId(userId: Long?) = apply { this.userId = userId }

    fun asGuest() = apply { this.userId = null }

    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withTitle(title: String) = apply { this.title = title }

    fun withContent(content: String) = apply { this.content = content }

    fun withAuthor(author: String) = apply { this.author = author }

    fun create(): BoardCreatedEventPayload =
        BoardCreatedEventPayload(
            boardId = boardId,
            userId = userId,
            categoryId = categoryId,
            createdAt = createdAt,
            eventTs = eventTs,
            title = title,
            content = content,
            author = author,
        )

    companion object {
        fun aBoardCreatedEventPayload() = BoardCreatedEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            categoryId: Long = 1L,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardCreatedEventPayload()
            .withBoardId(boardId)
            .withCategoryId(categoryId)
            .withEventTs(eventTs)
            .create()
    }
}

class BoardUpdatedEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var eventTs: Long = System.currentTimeMillis(),
    private var title: String = "수정된 페이로드 제목",
    private var content: String = "수정된 페이로드 내용",
    private var author: String = "수정된 페이로드 작성자", // Note: Author might not be updatable, adjust if needed
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withTitle(title: String) = apply { this.title = title }

    fun withContent(content: String) = apply { this.content = content }

    fun withAuthor(author: String) = apply { this.author = author }

    fun create(): BoardUpdatedEventPayload =
        BoardUpdatedEventPayload(
            boardId = boardId,
            eventTs = eventTs,
            title = title,
            content = content,
            author = author,
        )

    companion object {
        fun aBoardUpdatedEventPayload() = BoardUpdatedEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardUpdatedEventPayload()
            .withBoardId(boardId)
            .withEventTs(eventTs)
            .create()
    }
}

class BoardDeletedEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var eventTs: Long = System.currentTimeMillis(),
    private var categoryId: Long = 1L,
    private var userId: Long? = 10L,
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }

    fun withUserId(userId: Long?) = apply { this.userId = userId }

    fun asGuest() = apply { this.userId = null }

    fun create(): BoardDeletedEventPayload =
        BoardDeletedEventPayload(
            boardId = boardId,
            eventTs = eventTs,
            categoryId = categoryId,
            userId = userId,
        )

    companion object {
        fun aBoardDeletedEventPayload() = BoardDeletedEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            categoryId: Long = 1L,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardDeletedEventPayload()
            .withBoardId(boardId)
            .withCategoryId(categoryId)
            .withEventTs(eventTs)
            .create()
    }
}

class BoardLikedEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var createdAt: LocalDateTime = LocalDateTime.now(), // Typically event time
    private var eventTs: Long = System.currentTimeMillis(),
    private var userId: Long = 20L,
    private var likeCount: Int = 1, // Represents the state *after* liking
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withUserId(userId: Long) = apply { this.userId = userId }

    fun withLikeCount(likeCount: Int) = apply { this.likeCount = likeCount }

    fun create(): BoardLikedEventPayload =
        BoardLikedEventPayload(
            boardId = boardId,
            createdAt = createdAt,
            eventTs = eventTs,
            userId = userId,
            likeCount = likeCount,
        )

    companion object {
        fun aBoardLikedEventPayload() = BoardLikedEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            userId: Long = 20L,
            likeCount: Int = 1,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardLikedEventPayload()
            .withBoardId(boardId)
            .withUserId(userId)
            .withLikeCount(likeCount)
            .withEventTs(eventTs)
            .create()
    }
}

class BoardUnlikedEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var createdAt: LocalDateTime = LocalDateTime.now(), // Typically event time
    private var eventTs: Long = System.currentTimeMillis(),
    private var userId: Long = 20L,
    private var likeCount: Int = 0, // Represents the state *after* unliking
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withUserId(userId: Long) = apply { this.userId = userId }

    fun withLikeCount(likeCount: Int) = apply { this.likeCount = likeCount }

    fun create(): BoardUnlikedEventPayload =
        BoardUnlikedEventPayload(
            boardId = boardId,
            createdAt = createdAt,
            eventTs = eventTs,
            userId = userId,
            likeCount = likeCount,
        )

    companion object {
        fun aBoardUnlikedEventPayload() = BoardUnlikedEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            userId: Long = 20L,
            likeCount: Int = 0,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardUnlikedEventPayload()
            .withBoardId(boardId)
            .withUserId(userId)
            .withLikeCount(likeCount)
            .withEventTs(eventTs)
            .create()
    }
}

class BoardViewedEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var createdAt: LocalDateTime = LocalDateTime.now(), // Typically event time
    private var eventTs: Long = System.currentTimeMillis(),
    private var viewCount: Int = 1, // Represents the state *after* viewing
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withViewCount(viewCount: Int) = apply { this.viewCount = viewCount }

    fun create(): BoardViewedEventPayload =
        BoardViewedEventPayload(
            boardId = boardId,
            createdAt = createdAt,
            eventTs = eventTs,
            viewCount = viewCount,
        )

    companion object {
        fun aBoardViewedEventPayload() = BoardViewedEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            viewCount: Int = 1,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardViewedEventPayload()
            .withBoardId(boardId)
            .withViewCount(viewCount)
            .withEventTs(eventTs)
            .create()
    }
}

class BoardRankingUpdatedEventPayloadFixture private constructor(
    private var metric: BoardRankingMetric = BoardRankingMetric.VIEWS,
    private var period: BoardRankingPeriod = BoardRankingPeriod.DAY,
    private var rankings: List<BoardRankingUpdatedEventPayload.RankingEntry> =
        listOf(
            BoardRankingUpdatedEventPayload.RankingEntry(1L, 100.0),
            BoardRankingUpdatedEventPayload.RankingEntry(2L, 90.0),
        ),
    private var eventTs: Long = System.currentTimeMillis(),
) {
    fun withMetric(metric: BoardRankingMetric) = apply { this.metric = metric }

    fun withPeriod(period: BoardRankingPeriod) = apply { this.period = period }

    fun withRankings(rankings: List<BoardRankingUpdatedEventPayload.RankingEntry>) = apply { this.rankings = rankings }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun create(): BoardRankingUpdatedEventPayload =
        BoardRankingUpdatedEventPayload(
            metric = metric,
            period = period,
            rankings = rankings,
            eventTs = eventTs,
        )

    companion object {
        fun aBoardRankingUpdatedEventPayload() = BoardRankingUpdatedEventPayloadFixture()

        fun default(
            metric: BoardRankingMetric = BoardRankingMetric.VIEWS,
            period: BoardRankingPeriod = BoardRankingPeriod.DAY,
            eventTs: Long = System.currentTimeMillis(),
        ) = aBoardRankingUpdatedEventPayload()
            .withMetric(metric)
            .withPeriod(period)
            .withEventTs(eventTs)
            .create()
    }
}

// --- IncViewEventPayload Fixture ---
class IncViewEventPayloadFixture private constructor(
    private var boardId: Long = 1L,
    private var eventTs: Long = System.currentTimeMillis(),
    private var delta: Int = 1,
) {
    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun withDelta(delta: Int) = apply { this.delta = delta }

    fun create(): IncViewEventPayload =
        IncViewEventPayload(
            boardId = boardId,
            eventTs = eventTs,
            delta = delta,
        )

    companion object {
        fun aIncViewEventPayload() = IncViewEventPayloadFixture()

        // Convenience default method
        fun default(
            boardId: Long = 1L,
            delta: Int = 1,
            eventTs: Long = System.currentTimeMillis(),
        ) = aIncViewEventPayload()
            .withBoardId(boardId)
            .withDelta(delta)
            .withEventTs(eventTs)
            .create()
    }
}

class CommentCreatedEventPayloadFixture private constructor(
    private var commentId: Long = 101L,
    private var boardId: Long = 1L,
    private var userId: Long? = 20L,
    private var author: String = "댓글 페이로드 작성자",
    private var content: String = "댓글 페이로드 내용",
    private var createdAt: LocalDateTime = LocalDateTime.now().minusHours(1),
    private var eventTs: Long = System.currentTimeMillis(),
) {
    fun withCommentId(commentId: Long) = apply { this.commentId = commentId }

    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withUserId(userId: Long?) = apply { this.userId = userId }

    fun asGuest() = apply { this.userId = null }

    fun withAuthor(author: String) = apply { this.author = author }

    fun withContent(content: String) = apply { this.content = content }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun create(): com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload =
        com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload(
            commentId = commentId,
            boardId = boardId,
            userId = userId,
            author = author,
            content = content,
            createdAt = createdAt,
            eventTs = eventTs,
        )

    companion object {
        fun aCommentCreatedEventPayload() = CommentCreatedEventPayloadFixture()

        // Convenience default method
        fun default(
            commentId: Long = 101L,
            boardId: Long = 1L,
            eventTs: Long = System.currentTimeMillis(),
        ) = aCommentCreatedEventPayload()
            .withCommentId(commentId)
            .withBoardId(boardId)
            .withEventTs(eventTs)
            .create()
    }
}

// --- CommentUpdatedEventPayload Fixture ---
class CommentUpdatedEventPayloadFixture private constructor(
    private var commentId: Long = 101L,
    private var boardId: Long = 1L, // Usually needed for context, even if not directly used in update logic
    private var content: String = "수정된 댓글 페이로드 내용",
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
    private var eventTs: Long = System.currentTimeMillis(),
) {
    fun withCommentId(commentId: Long) = apply { this.commentId = commentId }

    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withContent(content: String) = apply { this.content = content }

    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun create(): CommentUpdatedEventPayload =
        CommentUpdatedEventPayload(
            commentId = commentId,
            boardId = boardId,
            content = content,
            updatedAt = updatedAt,
            eventTs = eventTs,
        )

    companion object {
        fun aCommentUpdatedEventPayload() = CommentUpdatedEventPayloadFixture()

        // Convenience default method
        fun default(
            commentId: Long = 101L,
            boardId: Long = 1L,
            eventTs: Long = System.currentTimeMillis(),
        ) = aCommentUpdatedEventPayload()
            .withCommentId(commentId)
            .withBoardId(boardId)
            .withEventTs(eventTs)
            .create()
    }
}

// --- CommentDeletedEventPayload Fixture ---
class CommentDeletedEventPayloadFixture private constructor(
    private var commentId: Long = 101L,
    private var boardId: Long = 1L,
    private var eventTs: Long = System.currentTimeMillis(),
) {
    fun withCommentId(commentId: Long) = apply { this.commentId = commentId }

    fun withBoardId(boardId: Long) = apply { this.boardId = boardId }

    fun withEventTs(eventTs: Long) = apply { this.eventTs = eventTs }

    fun create(): CommentDeletedEventPayload =
        CommentDeletedEventPayload(
            commentId = commentId,
            boardId = boardId,
            eventTs = eventTs,
        )

    companion object {
        fun aCommentDeletedEventPayload() = CommentDeletedEventPayloadFixture()

        // Convenience default method
        fun default(
            commentId: Long = 101L,
            boardId: Long = 1L,
            eventTs: Long = System.currentTimeMillis(),
        ) = aCommentDeletedEventPayload()
            .withCommentId(commentId)
            .withBoardId(boardId)
            .withEventTs(eventTs)
            .create()
    }
}
