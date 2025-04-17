package com.example.jobstat.core.event.payload.board

import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
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
