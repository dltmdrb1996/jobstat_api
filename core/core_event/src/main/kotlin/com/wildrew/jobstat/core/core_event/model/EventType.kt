package com.wildrew.jobstat.core.core_event.model

import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.comment.*
import com.wildrew.jobstat.core.core_event.model.payload.notification.EmailNotificationEvent
import org.slf4j.LoggerFactory

const val CONSUMER_NAME_COMMUNITY_READ = "community-read"
const val CONSUMER_NAME_COMMUNITY_COMMAND = "community-command"
const val CONSUMER_NAME_NOTIFICATION = "notification"

enum class EventType(
    val payloadClass: Class<out EventPayload>,
    val consumerName: String,
) {
    BOARD_CREATED(BoardCreatedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_UPDATED(BoardUpdatedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_DELETED(BoardDeletedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_LIKED(BoardLikedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_UNLIKED(BoardUnlikedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_VIEWED(BoardViewedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_RANKING_UPDATED(BoardRankingUpdatedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    BOARD_INC_VIEW(IncViewEventPayload::class.java, CONSUMER_NAME_COMMUNITY_COMMAND),

    COMMENT_CREATED(CommentCreatedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    COMMENT_UPDATED(CommentUpdatedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, CONSUMER_NAME_COMMUNITY_READ),

    EMAIL_NOTIFICATION(EmailNotificationEvent::class.java, CONSUMER_NAME_NOTIFICATION),
    ;

    fun getTopicName(): String = Topic.getTopicName(consumerName)

    fun getGroupId(): String = GroupId.getGroupId(consumerName)

    companion object {
        private val log by lazy { LoggerFactory.getLogger(EventType::class.java.enclosingClass) }

        fun from(type: String): EventType? =
            try {
                valueOf(type)
            } catch (e: Exception) {
                log.error("[이벤트 타입 변환 실패] type=$type", e)
                null
            }
    }

    object Topic {
        private lateinit var consumerConfigMap: Map<String, ConsumerConfig>
        private val log by lazy { LoggerFactory.getLogger(Topic::class.java) }

        fun initialize(configMap: Map<String, ConsumerConfig>) {
            this.consumerConfigMap = configMap
            log.info("[EventType.Topic Initialized] Using consumerConfigMap: $consumerConfigMap")
        }

        fun getTopicName(consumerNameKey: String): String {
            check(::consumerConfigMap.isInitialized) { "EventType.Topic has not been initialized. Ensure KafkaConsumersConfiguration is loaded." }
            return consumerConfigMap[consumerNameKey]?.topic
                ?: throw IllegalArgumentException("No topic configured for consumer name: $consumerNameKey. Available keys: ${consumerConfigMap.keys}")
        }

        val communityRead: String by lazy {
            log.debug("Lazily initializing EventType.Topic.communityRead")
            getTopicName(CONSUMER_NAME_COMMUNITY_READ)
        }
        val communityCommand: String by lazy {
            log.debug("Lazily initializing EventType.Topic.communityCommand")
            getTopicName(CONSUMER_NAME_COMMUNITY_COMMAND)
        }
        val notification: String by lazy {
            log.debug("Lazily initializing EventType.Topic.notification")
            getTopicName(CONSUMER_NAME_NOTIFICATION)
        }
    }

    object GroupId {
        private lateinit var consumerConfigMap: Map<String, ConsumerConfig>
        private val log by lazy { LoggerFactory.getLogger(GroupId::class.java) }

        fun initialize(configMap: Map<String, ConsumerConfig>) {
            this.consumerConfigMap = configMap
            log.info("[EventType.GroupId Initialized] Using consumerConfigMap: $consumerConfigMap")
        }

        fun getGroupId(consumerNameKey: String): String {
            check(::consumerConfigMap.isInitialized) { "EventType.GroupId has not been initialized." }
            return consumerConfigMap[consumerNameKey]?.groupId
                ?: throw IllegalArgumentException("No groupId configured for consumer name: $consumerNameKey. Available keys: ${consumerConfigMap.keys}")
        }

        val communityRead: String by lazy {
            log.debug("Lazily initializing EventType.GroupId.communityRead")
            getGroupId(CONSUMER_NAME_COMMUNITY_READ)
        }
        val communityCommand: String by lazy {
            log.debug("Lazily initializing EventType.GroupId.communityCommand")
            getGroupId(CONSUMER_NAME_COMMUNITY_COMMAND)
        }
        val notification: String by lazy {
            log.debug("Lazily initializing EventType.GroupId.notification")
            getGroupId(CONSUMER_NAME_NOTIFICATION)
        }
    }
}
