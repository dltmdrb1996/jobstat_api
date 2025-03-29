package com.example.jobstat.core.event.payload

import com.example.jobstat.core.event.EventPayload
import java.time.LocalDateTime

data class ArticleLikedEventPayload(
    val articleLikeId: Long,
    val articleId: Long,
    val userId: Long,
    val createdAt: LocalDateTime,
    val articleLikeCount: Long
) : EventPayload