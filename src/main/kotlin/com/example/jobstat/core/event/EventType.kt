package com.example.jobstat.core.event

import org.slf4j.LoggerFactory
import com.example.jobstat.core.event.payload.*

enum class EventType(
    val payloadClass: Class<out EventPayload>,
    val topic: String
) {
    ARTICLE_CREATED(ArticleCreatedEventPayload::class.java, Topic.ARTICLE),
    ARTICLE_UPDATED(ArticleUpdatedEventPayload::class.java, Topic.ARTICLE),
    ARTICLE_DELETED(ArticleDeletedEventPayload::class.java, Topic.ARTICLE),
    COMMENT_CREATED(CommentCreatedEventPayload::class.java, Topic.COMMENT),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, Topic.COMMENT),
    ARTICLE_LIKED(ArticleLikedEventPayload::class.java, Topic.LIKE),
    ARTICLE_UNLIKED(ArticleUnlikedEventPayload::class.java, Topic.LIKE),
    ARTICLE_VIEWED(ArticleViewedEventPayload::class.java, Topic.VIEW);

    companion object {
        private val logger = LoggerFactory.getLogger(EventType::class.java)

        fun from(type: String): EventType? {
            return try {
                valueOf(type)
            } catch (e: Exception) {
                logger.error("[EventType.from] type={}", type, e)
                null
            }
        }
    }

    object Topic {
        const val ARTICLE = "jobstat-article"
        const val COMMENT = "jobstat-comment"
        const val LIKE = "jobstat-like"
        const val VIEW = "jobstat-view"
    }
}