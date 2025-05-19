package com.example.jobstat.core.core_event.model.payload.board

import com.example.jobstat.core.core_event.model.EventPayload
import com.example.jobstat.core.core_model.BoardRankingMetric
import com.example.jobstat.core.core_model.BoardRankingPeriod
import com.fasterxml.jackson.annotation.JsonProperty

data class BoardRankingUpdatedEventPayload(
    @JsonProperty("metric")
    val metric: BoardRankingMetric,
    @JsonProperty("period")
    val period: BoardRankingPeriod,
    @JsonProperty("rankings")
    val rankings: List<RankingEntry>,
    @JsonProperty("eventTs")
    val eventTs: Long,
) : EventPayload {
    data class RankingEntry(
        @JsonProperty("boardId")
        val boardId: Long,
        @JsonProperty("score")
        val score: Double,
    )
}
