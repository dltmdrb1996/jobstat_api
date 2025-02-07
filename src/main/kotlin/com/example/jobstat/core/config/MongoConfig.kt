package com.example.jobstat.core.config

import com.example.jobstat.core.converter.RankingScoreReadConverter
import com.example.jobstat.core.converter.RankingScoreWriteConverter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

@Configuration
class MongoConfig(
    private val objectMapper: ObjectMapper,
    private val rankingScoreReadConverter: RankingScoreReadConverter,
    private val rankingScoreWriteConverter: RankingScoreWriteConverter,
) {
    @Bean
    fun mappingMongoConverter(mongoMappingContext: MongoMappingContext): MappingMongoConverter {
        val converter = MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mongoMappingContext)
        converter.setTypeMapper(DefaultMongoTypeMapper(null)) // 기본 _class 필드 비활성화

        // 커스텀 컨버터 등록
        val converters: List<Converter<*, *>> =
            listOf(
                rankingScoreReadConverter,
                rankingScoreWriteConverter,
            )
        converter.setCustomConversions(MongoCustomConversions(converters))

        converter.afterPropertiesSet()
        return converter
    }
}
