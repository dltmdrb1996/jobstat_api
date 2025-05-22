package com.wildrew.jobstat.core.core_event.model

import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import org.slf4j.LoggerFactory

enum class EventType(
    val payloadClass: Class<out EventPayload>,
    val topic: String,
) {
    // 게시글 생성, 수정, 삭제
    BOARD_CREATED(BoardCreatedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_UPDATED(BoardUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_DELETED(BoardDeletedEventPayload::class.java, Topic.COMMUNITY_READ),

    // 게시글 상호작용 (좋아요, 조회 등)
    BOARD_LIKED(com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_UNLIKED(BoardUnlikedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_VIEWED(BoardViewedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_RANKING_UPDATED(com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),

    // 게시글 카운터 증가 명령
    BOARD_INC_VIEW(IncViewEventPayload::class.java, Topic.COMMUNITY_COMMAND),

    COMMENT_CREATED(com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload::class.java, Topic.COMMUNITY_READ),
    COMMENT_UPDATED(CommentUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, Topic.COMMUNITY_READ),
    ;

    companion object {
        private val log by lazy { LoggerFactory.getLogger(this::class.java) }

        fun from(type: String): EventType? =
            try {
                valueOf(type)
            } catch (e: Exception) {
                log.error("[이벤트 타입 변환 실패] type=$type", e)
                null
            }
    }

    object Topic {
        const val COMMUNITY_COMMAND = "community-command"
        const val COMMUNITY_READ = "community-read"
    }

    object GroupId {
        const val COMMUNITY_COMMAND = "community-command-consumer-group"
        const val COMMUNITY_READ = "community-read-consumer-group"
    }
}
