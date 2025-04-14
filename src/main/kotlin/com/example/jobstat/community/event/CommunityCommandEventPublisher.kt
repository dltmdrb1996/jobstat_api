package com.example.jobstat.community.event

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.outbox.OutboxEventPublisher
import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*
import com.example.jobstat.core.event.publisher.AbstractEventPublisher
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 커뮤니티 관련 이벤트 발행을 담당하는 퍼블리셔
 * 게시판 및 댓글 서비스에서 발생하는 모든 이벤트를 관리
 */
@Component
internal class CommunityCommandEventPublisher(
    outboxEventPublisher: OutboxEventPublisher,
) : AbstractEventPublisher(outboxEventPublisher) {
    /**
     * 이 퍼블리셔가 지원하는 이벤트 타입들
     */
    override fun getSupportedEventTypes(): Set<EventType> = SUPPORTED_EVENT_TYPES

    companion object {
        private val SUPPORTED_EVENT_TYPES =
            setOf(
                // 게시판 이벤트 타입
                EventType.BOARD_CREATED,
                EventType.BOARD_UPDATED,
                EventType.BOARD_DELETED,
                EventType.BOARD_LIKED,
                EventType.BOARD_UNLIKED,
                EventType.BOARD_RANKING_UPDATED,
                EventType.BOARD_VIEWED,
                // 댓글 이벤트 타입
                EventType.COMMENT_CREATED,
                EventType.COMMENT_UPDATED,
                EventType.COMMENT_DELETED,
            )
    }

    fun publishBoardCreated(
        board: Board,
        categoryId: Long,
        userId: Long? = null,
    ) {
        val payload =
            BoardCreatedEventPayload(
                boardId = board.id,
                userId = userId,
                categoryId = categoryId,
                createdAt = board.createdAt,
                title = board.title,
                content = board.content,
                author = board.author,
                eventTs = System.currentTimeMillis(),
            )
        publish(EventType.BOARD_CREATED, payload)
    }

    /**
     * 게시글 수정 이벤트 발행
     */
    fun publishBoardUpdated(board: Board) {
        val payload =
            BoardUpdatedEventPayload(
                boardId = board.id,
                title = board.title,
                content = board.content,
                author = board.author,
                eventTs = System.currentTimeMillis(),
            )
        publish(EventType.BOARD_UPDATED, payload)
    }

    /**
     * 게시글 삭제 이벤트 발행
     */
    fun publishBoardDeleted(
        boardId: Long,
        userId: Long? = null,
        categoryId: Long,
        eventTs: Long = System.currentTimeMillis(),
    ) {
        val payload =
            BoardDeletedEventPayload(
                boardId = boardId,
                categoryId = categoryId,
                userId = userId,
                eventTs = eventTs,
            )
        publish(EventType.BOARD_DELETED, payload)
    }

    /**
     * 게시글 좋아요 이벤트 발행
     */
    fun publishBoardLiked(
        boardId: Long,
        createdAt: LocalDateTime,
        userId: Long,
        likeCount: Int,
        eventTs: Long = System.currentTimeMillis(),
    ) {
        val payload =
            BoardLikedEventPayload(
                boardId = boardId,
                createdAt = createdAt,
                eventTs = eventTs,
                userId = userId,
                likeCount = likeCount,
            )
        publish(EventType.BOARD_LIKED, payload)
    }

    /**
     * 게시글 좋아요 취소 이벤트 발행
     */
    fun publishBoardUnliked(
        boardId: Long,
        createdAt: LocalDateTime,
        userId: Long,
        likeCount: Int,
        eventTs: Long = System.currentTimeMillis(),
    ) {
        val payload =
            BoardUnlikedEventPayload(
                boardId = boardId,
                createdAt = createdAt,
                userId = userId,
                eventTs = eventTs,
                likeCount = likeCount,
            )
        // BOARD_UNLIKED 이벤트 타입으로 변경
        publish(EventType.BOARD_UNLIKED, payload)
    }

    /**
     * 게시글 조회수 업데이트 이벤트 발행
     */
    fun publishBoardViewed(
        boardId: Long,
        createdAt: LocalDateTime,
        viewCount: Int,
        eventTs: Long = System.currentTimeMillis(),
    ) {
        val payload =
            BoardViewedEventPayload(
                boardId = boardId,
                createdAt = createdAt,
                eventTs = eventTs,
                viewCount = viewCount,
            )
        publish(EventType.BOARD_VIEWED, payload)
    }

    /**
     * 댓글 생성 이벤트 발행
     */
    fun publishCommentCreated(
        comment: Comment,
        boardId: Long,
        userId: Long?,
    ) {
        val payload =
            CommentCreatedEventPayload(
                commentId = comment.id,
                boardId = boardId,
                userId = userId,
                author = comment.author,
                content = comment.content,
                createdAt = comment.createdAt,
                eventTs = System.currentTimeMillis(),
            )
        publish(EventType.COMMENT_CREATED, payload)
    }

    /**
     * 댓글 수정 이벤트 발행
     */
    fun publishCommentUpdated(
        comment: Comment,
        boardId: Long,
    ) {
        val payload =
            CommentUpdatedEventPayload(
                commentId = comment.id,
                boardId = boardId,
                content = comment.content,
                updatedAt = comment.updatedAt,
                eventTs = System.currentTimeMillis(),
            )
        publish(EventType.COMMENT_UPDATED, payload)
    }

    /**
     * 댓글 삭제 이벤트 발행
     */
    fun publishCommentDeleted(
        commentId: Long,
        boardId: Long,
    ) {
        val payload =
            CommentDeletedEventPayload(
                commentId = commentId,
                boardId = boardId,
                eventTs = System.currentTimeMillis(),
            )
        publish(EventType.COMMENT_DELETED, payload)
    }

    fun publishBoardRankingUpdated(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        rankings: List<BoardRankingUpdatedEventPayload.RankingEntry>,
    ) {
        val payload =
            BoardRankingUpdatedEventPayload(
                metric = metric,
                period = period,
                rankings = rankings,
                eventTs = System.currentTimeMillis(),
            )
        publish(EventType.BOARD_RANKING_UPDATED, payload)
    }
}
