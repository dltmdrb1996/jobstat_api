package com.example.jobstat.core.base.mongo.converter

import com.example.jobstat.core.base.mongo.stats.RankingScore
import com.fasterxml.jackson.databind.ObjectMapper
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
