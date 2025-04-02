package com.example.jobstat.core.event

import com.example.jobstat.core.event.payload.comment.CommentCreatedEventPayload
import com.example.jobstat.core.event.payload.comment.CommentDeletedEventPayload
import org.slf4j.LoggerFactory
import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.CommentUpdatedEventPayload

enum class EventType(
    val payloadClass: Class<out EventPayload>,
    val topic: String
) {
    // 커뮤니티 관련 이벤트 타입
    BOARD_CREATED(BoardCreatedEventPayload::class.java, Topic.COMMUNITY_BOARD),
    BOARD_UPDATED(BoardUpdatedEventPayload::class.java, Topic.COMMUNITY_BOARD),
    BOARD_DELETED(BoardDeletedEventPayload::class.java, Topic.COMMUNITY_BOARD),
    BOARD_VIEWED(BoardViewedEventPayload::class.java, Topic.COMMUNITY_BOARD),
    BOARD_LIKED(BoardLikedEventPayload::class.java, Topic.COMMUNITY_BOARD),
    BOARD_UNLIKED(BoardUnlikedEventPayload::class.java, Topic.COMMUNITY_BOARD),

    COMMENT_CREATED(CommentCreatedEventPayload::class.java, Topic.COMMUNITY_COMMENT),
    COMMENT_UPDATED(CommentUpdatedEventPayload::class.java, Topic.COMMUNITY_COMMENT),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, Topic.COMMUNITY_COMMENT);

    companion object {
        private val log by lazy { LoggerFactory.getLogger(this::class.java) }

        fun from(type: String): EventType? {
            return try {
                valueOf(type)
            } catch (e: Exception) {
                log.error("[EventType.from] type={}", type, e)
                null
            }
        }
    }

    object Topic {
        const val COMMUNITY_BOARD = "community-board"
        const val COMMUNITY_COMMENT = "community-comment"
        const val COMMUNITY_READ = "community-read"
    }

    object GROUP_ID {
        const val COMMUNITY_BOARD = "community-board-service"
        const val COMMUNITY_COMMENT = "community-comment-service"
        const val COMMUNITY_READ = "community-read-service"
    }
}