package com.wildrew.jobstat.core.core_event.model.payload.board.item

import com.fasterxml.jackson.annotation.JsonProperty

data class BoardIncrementItem(
    @JsonProperty("boardId")
    val boardId: Long,
    @JsonProperty("viewIncrement")
    val viewIncrement: Int,
    @JsonProperty("likeIncrement")
    val likeIncrement: Int,
)
