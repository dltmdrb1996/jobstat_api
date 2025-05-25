package com.wildrew.jobstat.core.core_event.model

import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.comment.*
import com.wildrew.jobstat.core.core_event.model.payload.notification.EmailNotificationEvent
import org.slf4j.LoggerFactory

enum class TopicKey {
    COMMUNITY_READ,
    COMMUNITY_COMMAND,
    NOTIFICATION,
}

enum class EventType(
    val payloadClass: Class<out EventPayload>,
    val topicKey: TopicKey,
) {
    BOARD_CREATED(BoardCreatedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_UPDATED(BoardUpdatedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_DELETED(BoardDeletedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_LIKED(BoardLikedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_UNLIKED(BoardUnlikedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_VIEWED(BoardViewedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_RANKING_UPDATED(BoardRankingUpdatedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    BOARD_INC_VIEW(IncViewEventPayload::class.java, TopicKey.COMMUNITY_COMMAND),
    COMMENT_CREATED(CommentCreatedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    COMMENT_UPDATED(CommentUpdatedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, TopicKey.COMMUNITY_READ),
    EMAIL_NOTIFICATION(EmailNotificationEvent::class.java, TopicKey.NOTIFICATION),
    ;

    fun getTopicName(): String =
        when (topicKey) {
            TopicKey.COMMUNITY_READ -> Topic.communityRead
            TopicKey.COMMUNITY_COMMAND -> Topic.communityCommand
            TopicKey.NOTIFICATION -> Topic.notification
        }

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
        private const val COMMUNITY_COMMAND_BASE = "community-command"
        private const val COMMUNITY_READ_BASE = "community-read"
        private const val NOTIFICATION_BASE = "notification"

        lateinit var communityCommand: String
        lateinit var communityRead: String
        lateinit var notification: String

        private val log by lazy { LoggerFactory.getLogger(Topic::class.java) }

        fun initialize(activeProfile: String) {
            communityCommand = "$COMMUNITY_COMMAND_BASE-$activeProfile"
            communityRead = "$COMMUNITY_READ_BASE-$activeProfile"
            notification = "$NOTIFICATION_BASE-$activeProfile"
            log.info("[EventType.Topic Initialized] Active Profile: $activeProfile, communityRead: $communityRead, communityCommand: $communityCommand, notification: $notification") // Updated log to use camelCase
        }
    }

    object GroupId {
        private const val COMMUNITY_COMMAND_BASE = "community-command-consumer-group"
        private const val COMMUNITY_READ_BASE = "community-read-consumer-group"
        private const val NOTIFICATION_BASE = "notification-consumer-group"

        private lateinit var communityCommand: String
        private lateinit var communityRead: String
        private lateinit var notification: String

        private val log by lazy { LoggerFactory.getLogger(GroupId::class.java) }

        fun initialize(activeProfile: String) {
            communityCommand = "$COMMUNITY_COMMAND_BASE-$activeProfile"
            communityRead = "$COMMUNITY_READ_BASE-$activeProfile"
            notification = "$NOTIFICATION_BASE-$activeProfile"
            log.info("[EventType.GroupId Initialized] Active Profile: $activeProfile, communityRead: $communityRead, communityCommand: $communityCommand, notification: $notification")
        }
    }
}
