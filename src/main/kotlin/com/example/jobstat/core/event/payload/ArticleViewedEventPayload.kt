package com.example.jobstat.core.event.payload

import com.example.jobstat.core.event.EventPayload

data class ArticleViewedEventPayload(
    val articleId: Long,
    val articleViewCount: Long
) : EventPayload