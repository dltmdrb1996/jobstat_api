package com.wildrew.app.statistics_read.core.core_mongo_base.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.RankingScore
import org.bson.Document
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class RankingScoreWriteConverter(
    private val objectMapper: ObjectMapper,
) : Converter<RankingScore, Document> {
    override fun convert(source: RankingScore): Document = Document.parse(objectMapper.writeValueAsString(source))
}
