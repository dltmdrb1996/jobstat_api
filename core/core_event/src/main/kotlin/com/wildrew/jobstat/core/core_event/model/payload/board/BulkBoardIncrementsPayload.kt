package com.wildrew.jobstat.core.core_event.model.payload.board

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.payload.board.item.BoardIncrementItem

data class BulkBoardIncrementsPayload(
    @JsonProperty("batchId")
    val batchId: String,
    @JsonProperty("items")
    val items: List<BoardIncrementItem>,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload
