package com.wildrew.jobstat.core.core_event.model.payload.notification

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload

data class EmailNotificationEvent(
    @JsonProperty("to")
    val to: String,
    @JsonProperty("subject")
    val subject: String,
    @JsonProperty("body")
    val body: String,
) : EventPayload
