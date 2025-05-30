package com.wildrew.jobstat.statistics_read.core.core_mongo_base.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.RankingScore
import org.bson.Document
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class RankingScoreReadConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Document, RankingScore> {
    override fun convert(source: Document): RankingScore? {
        val type = source.getString("score_type") ?: return null
        return try {
            objectMapper.readValue(source.toJson(), RankingScore::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deserialize RankingScore with type: $type", e)
        }
    }
}
