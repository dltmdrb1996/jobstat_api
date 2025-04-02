package com.example.jobstat.community

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*
import com.example.jobstat.core.event.publisher.AbstractEventPublisher
import com.example.jobstat.core.event.outbox.OutboxEventPublisher
import com.example.jobstat.core.global.extension.toEpochMilli
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 커뮤니티 관련 이벤트 발행을 담당하는 퍼블리셔
 * 게시판 및 댓글 서비스에서 발생하는 모든 이벤트를 관리
 */
@Component
internal class CommunityEventPublisher(
    outboxEventPublisher: OutboxEventPublisher
) : AbstractEventPublisher(outboxEventPublisher) {

    /**
     * 이 퍼블리셔가 지원하는 이벤트 타입들
     */
    override fun getSupportedEventTypes(): Set<EventType> = SUPPORTED_EVENT_TYPES

    companion object {
        private val SUPPORTED_EVENT_TYPES = setOf(
            // 게시판 이벤트 타입
            EventType.BOARD_CREATED,
            EventType.BOARD_UPDATED,
            EventType.BOARD_DELETED,
            EventType.BOARD_VIEWED,
            EventType.BOARD_LIKED,
            EventType.BOARD_UNLIKED,

            // 댓글 이벤트 타입
            EventType.COMMENT_CREATED,
            EventType.COMMENT_UPDATED,
            EventType.COMMENT_DELETED
        )
    }

    fun publishBoardCreated(board: Board, categoryId: Long, userId: Long? = null) {
        val payload = BoardCreatedEventPayload(
            boardId = board.id,
            userId = userId,
            categoryId = categoryId,
            createdAt = board.createdAt,
            eventTs = board.updatedAt.toEpochMilli(),
            title = board.title,
            content = board.content,
            author = board.author
        )
        publish(EventType.BOARD_CREATED, payload, board.id)
    }

    /**
     * 게시글 수정 이벤트 발행
     */
    fun publishBoardUpdated(board: Board) {
        val payload = BoardUpdatedEventPayload(
            boardId = board.id,
            eventTs = board.updatedAt.toEpochMilli(),
            title = board.title,
            content = board.content,
            author = board.author
        )
        publish(EventType.BOARD_UPDATED, payload, board.id)
    }

    /**
     * 게시글 삭제 이벤트 발행
     */
    fun publishBoardDeleted(boardId: Long, userId: Long? = null, categoryId: Long, eventTs: Long) {
        val payload = BoardDeletedEventPayload(
            boardId = boardId,
            eventTs = eventTs,
            categoryId = categoryId,
            userId = userId,
        )
        publish(EventType.BOARD_DELETED, payload, boardId)
    }

    /**
     * 게시글 좋아요 이벤트 발행
     */
    fun publishBoardLiked(boardId: Long, createdAt: LocalDateTime, eventTs: Long, userId: Long, likeCount: Int) {
        val payload = BoardLikedEventPayload(
            boardId = boardId,
            createdAt = createdAt,
            eventTs = eventTs,
            userId = userId,
            likeCount = likeCount,
        )
        publish(EventType.BOARD_LIKED, payload, boardId)
    }

    /**
     * 게시글 좋아요 취소 이벤트 발행
     */
    fun publishBoardUnliked(boardId: Long, createdAt: LocalDateTime, userId: Long, eventTs: Long,  likeCount: Int) {
        val payload = BoardUnlikedEventPayload(
            boardId = boardId,
            createdAt = createdAt,
            userId = userId,
            eventTs = eventTs,
            likeCount = likeCount
        )
        // BOARD_UNLIKED 이벤트 타입으로 변경
        publish(EventType.BOARD_UNLIKED, payload, boardId)
    }

    /**
     * 게시글 조회수 업데이트 이벤트 발행
     */
    fun publishBoardViewed(
        boardId: Long,
        createdAt: LocalDateTime,
        eventTs: Long,
        viewCount : Int,
    ) {
        val payload = BoardViewedEventPayload(
            boardId = boardId,
            createdAt = createdAt,
            eventTs = eventTs,
            viewCount = viewCount,
        )
        publish(EventType.BOARD_VIEWED, payload, boardId)
    }

    /**
     * 댓글 생성 이벤트 발행
     */
    fun publishCommentCreated(
        comment: Comment,
        boardId: Long,
        path: String,
        articleId: Long,
        writerId: Long,
        deleted: Boolean,
        createdAt: LocalDateTime,
        articleCommentCount: Long
    ) {
        val payload = CommentCreatedEventPayload(
            commentId = comment.id,
            boardId = boardId,
            content = comment.content,
            path = path,
            author = comment.author,
            articleId = articleId,
            writerId = writerId,
            deleted = deleted,
            createdAt = createdAt,
            articleCommentCount = articleCommentCount
        )
        publish(EventType.COMMENT_CREATED, payload, comment.id)
    }

    /**
     * 댓글 수정 이벤트 발행
     */
    fun publishCommentUpdated(
        comment: Comment,
        boardId: Long,
        path: String,
        articleId: Long,
        writerId: Long,
        deleted: Boolean,
        createdAt: LocalDateTime,
        articleCommentCount: Long
    ) {
        val payload = CommentUpdatedEventPayload(
            commentId = comment.id,
            boardId = boardId,
            content = comment.content,
            path = path,
            author = comment.author,
            articleId = articleId,
            writerId = writerId,
            deleted = deleted,
            createdAt = createdAt,
            articleCommentCount = articleCommentCount
        )
        publish(EventType.COMMENT_UPDATED, payload, comment.id)
    }

    /**
     * 댓글 삭제 이벤트 발행
     */
    fun publishCommentDeleted(
        commentId: Long,
        boardId: Long,
        content: String,
        path: String,
        articleId: Long,
        writerId: Long,
        deleted: Boolean,
        createdAt: LocalDateTime,
        articleCommentCount: Long
    ) {
        val payload = CommentDeletedEventPayload(
            commentId = commentId,
            boardId = boardId,
            content = content,
            path = path,
            articleId = articleId,
            writerId = writerId,
            deleted = deleted,
            createdAt = createdAt,
            articleCommentCount = articleCommentCount
        )
        publish(EventType.COMMENT_DELETED, payload, commentId)
    }
}