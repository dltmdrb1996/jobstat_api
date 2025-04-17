package com.example.jobstat.core.config

import com.example.jobstat.core.base.mongo.converter.RankingScoreReadConverter
import com.example.jobstat.core.base.mongo.converter.RankingScoreWriteConverter
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
    private val rankingScoreReadConverter: RankingScoreReadConverter,
    private val rankingScoreWriteConverter: RankingScoreWriteConverter,
) {
    @Bean
    fun mappingMongoConverter(mongoMappingContext: MongoMappingContext): MappingMongoConverter {
        val converter = MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mongoMappingContext)
        converter.setTypeMapper(DefaultMongoTypeMapper(null))

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
