package com.example.jobstat.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate


@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.database}") private val dataIndex: Int
) {

    @Bean
    fun createLettuceConnectionFactory() : RedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.port = port
        redisStandaloneConfiguration.database = dataIndex
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

//    @Bean
//    fun redisTemplate(
//        redisConnectionFactory: RedisConnectionFactory,
//        objectMapper: ObjectMapper,
//    ): StringRedisTemplate {
//        val template = StringRedisTemplate()
//        template.connectionFactory = redisConnectionFactory
//
//        val jackson2JsonRedisSerializer = Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
//        template.keySerializer = StringRedisSerializer()
//        template.valueSerializer = jackson2JsonRedisSerializer
//        template.hashKeySerializer = StringRedisSerializer()
//        template.hashValueSerializer = jackson2JsonRedisSerializer
//
//        return template
//    }

    @Bean
    fun stringRedisTemplate(): StringRedisTemplate {
        return StringRedisTemplate(createLettuceConnectionFactory())
    }

//    @Bean
//    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
//        val cacheConfigurations: MutableMap<String, RedisCacheConfiguration> = HashMap()
//        cacheConfigurations[ONE_DAY_CACHE] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1))
//        cacheConfigurations[INSTRUMENT_CACHE] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(366))
//        cacheConfigurations[THIRTY_MINUTES] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
//        cacheConfigurations[SUMMARY_CACHE] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(6))
//        cacheConfigurations[SUMMARY_CACHE_15] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15))
//        cacheConfigurations[TRANSACTION_CACHE] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1))
//        cacheConfigurations[USER_SESSION_CACHE] = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1))
//        cacheConfigurations[USER_SESSION_ID_CACHE] =
//            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1))
//        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL)
//        return RedisCacheManager.builder(connectionFactory)
//            .cacheDefaults(defaultConfig)
//            .withInitialCacheConfigurations(cacheConfigurations)
//            .build()
//    }
//
//    companion object {
//        const val INSTRUMENT_CACHE = "instrument-cache-v23"
//        const val SUMMARY_CACHE = "summary-cache-v23"
//        const val SUMMARY_CACHE_15 = "summary-cache-15-v23"
//        const val TRANSACTION_CACHE = "transaction-cache-v23"
//        const val ONE_DAY_CACHE: String = "one-day-cache-v23"
//        const val THIRTY_MINUTES: String = "thirty-minutes-cache-v23"
//        const val USER_SESSION_CACHE: String = "user-session-cache-v23"
//        const val USER_SESSION_ID_CACHE: String = "user-session-id-cache-v23"
//        private val DEFAULT_TTL: Duration = Duration.ofMinutes(30)
//    }
}


//
//package com.example.jobstat.core.config
//
//import com.example.jobstat.core.constants.CacheConstants
//import com.fasterxml.jackson.databind.ObjectMapper
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.cache.annotation.EnableCaching
//import org.springframework.cache.support.CompositeCacheManager
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.redis.cache.RedisCacheConfiguration
//import org.springframework.data.redis.cache.RedisCacheManager
//import org.springframework.data.redis.connection.RedisConnectionFactory
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
//import org.springframework.data.redis.core.RedisTemplate
//import org.springframework.data.redis.core.StringRedisTemplate
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
//import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
//import org.springframework.data.redis.serializer.RedisSerializationContext
//import org.springframework.data.redis.serializer.StringRedisSerializer
//import java.time.Duration
//
//@Configuration
//@EnableCaching
//class RedisConfig(
//    @Value("\${spring.redis.host}") private val host: String,
//    @Value("\${spring.redis.port}") private val port: Int,
//    @Value("\${spring.data.redis.database}") private val dataIndex: Int
//) {
//    @Bean
//    fun createLettuceConnectionFactory(): RedisConnectionFactory {
//        val redisStandaloneConfiguration = RedisStandaloneConfiguration().apply {
//            hostName = host
//            port = port
//            database = dataIndex
//        }
//
//        // Ranking Cache를 위한 Redis 인스턴스 (0번 DB)
//        return LettuceConnectionFactory(redisStandaloneConfiguration).apply {
//            // maxmemory-policy를 noeviction으로 설정 (고정 캐시)
//            afterPropertiesSet()
//        }
//    }
//
//    @Bean
//    fun createStatsLettuceConnectionFactory(): RedisConnectionFactory {
//        val redisStandaloneConfiguration = RedisStandaloneConfiguration().apply {
//            hostName = host
//            port = port
//            database = 1 // Detail Cache를 위한 별도 DB (1번 DB)
//        }
//
//        return LettuceConnectionFactory(redisStandaloneConfiguration).apply {
//            // maxmemory-policy를 allkeys-lru로 설정
//            afterPropertiesSet()
//        }
//    }
//
//    @Bean
//    fun redisCacheManager(
//        rankingRedisConnectionFactory: RedisConnectionFactory,
//        detailRedisConnectionFactory: RedisConnectionFactory
//    ): CompositeCacheManager {
//        // 기본 캐시 설정
//        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
//            .serializeKeysWith(
//                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
//            )
//            .serializeValuesWith(
//                RedisSerializationContext.SerializationPair.fromSerializer(
//                    GenericJackson2JsonRedisSerializer()
//                )
//            )
//            .disableCachingNullValues()
//
//        // Ranking 캐시 설정 (고정 캐시)
//        val rankingCacheConfig = defaultConfig
//            .entryTtl(Duration.ofHours(CacheConstants.RANKING_TTL_HOURS))
//
//        // Detail 캐시 설정 (LRU)
//        val detailCacheConfig = defaultConfig
//            .entryTtl(Duration.ofMinutes(CacheConstants.DETAIL_TTL_MINUTES))
//
//        // Ranking 캐시 매니저 설정
//        val rankingCacheManager = RedisCacheManager.builder(rankingRedisConnectionFactory)
//            .cacheDefaults(rankingCacheConfig)
//            .withCacheConfiguration(CacheConstants.RANKING_CACHE, rankingCacheConfig)
//            .build()
//
//        // Detail 캐시 매니저 설정
//        val detailCacheManager = RedisCacheManager.builder(detailRedisConnectionFactory)
//            .cacheDefaults(detailCacheConfig)
//            .withCacheConfiguration(CacheConstants.DETAIL_CACHE, detailCacheConfig)
//            .build()
//
//        // 복합 캐시 매니저 반환
//        return CompositeCacheManager().apply {
//            setCacheManagers(listOf(rankingCacheManager, detailCacheManager))
//            setFallbackToNoOpCache(false)
//        }
//    }
//
//    @Bean
//    fun rankingRedisTemplate(rankingRedisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
//        return RedisTemplate<String, Any>().apply {
//            setConnectionFactory(rankingRedisConnectionFactory)
//            keySerializer = StringRedisSerializer()
//            valueSerializer = GenericJackson2JsonRedisSerializer()
//            hashKeySerializer = StringRedisSerializer()
//            hashValueSerializer = GenericJackson2JsonRedisSerializer()
//            afterPropertiesSet()
//        }
//    }
//
//    @Bean
//    fun detailRedisTemplate(detailRedisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
//        return RedisTemplate<String, Any>().apply {
//            setConnectionFactory(detailRedisConnectionFactory)
//            keySerializer = StringRedisSerializer()
//            valueSerializer = GenericJackson2JsonRedisSerializer()
//            hashKeySerializer = StringRedisSerializer()
//            hashValueSerializer = GenericJackson2JsonRedisSerializer()
//            afterPropertiesSet()
//        }
//    }
//}
