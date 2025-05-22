package com.wildrew.jobstat.core.core_event.model.payload.board

import com.fasterxml.jackson.annotation.JsonProperty
import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
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
