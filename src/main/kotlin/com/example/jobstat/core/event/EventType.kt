package com.example.jobstat.core.event

import com.example.jobstat.core.event.payload.comment.CommentCreatedEventPayload
import com.example.jobstat.core.event.payload.comment.CommentDeletedEventPayload
import org.slf4j.LoggerFactory
import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.CommentUpdatedEventPayload

/**
 * 이벤트 타입 열거형
 */
enum class EventType(
    val payloadClass: Class<out EventPayload>,
    val topic: String
) {
    // 커뮤니티 관련 이벤트 타입
    BOARD_CREATED(BoardCreatedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_UPDATED(BoardUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_DELETED(BoardDeletedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_LIKED(BoardLikedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_UNLIKED(BoardUnlikedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_VIEWED(BoardViewedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_RANKING_UPDATED(BoardRankingUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),

    BOARD_INC_VIEW(IncViewEventPayload::class.java, Topic.COMMUNITY_COMMAND),

    COMMENT_CREATED(CommentCreatedEventPayload::class.java, Topic.COMMUNITY_READ),
    COMMENT_UPDATED(CommentUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, Topic.COMMUNITY_READ);

    companion object {
        private val log by lazy { LoggerFactory.getLogger(this::class.java) }

        fun from(type: String): EventType? {
            return try {
                valueOf(type)
            } catch (e: Exception) {
                log.error("[EventType.from] type=${type}", e)
                null
            }
        }
    }

    object Topic {
        const val COMMUNITY_COMMAND = "community-command"
        const val COMMUNITY_READ = "community-read"
    }

    object GROUP_ID {
        const val COMMUNITY_COMMAND = "community-command-consumer-group"
        const val COMMUNITY_READ = "community-read-consumer-group"
    }
}