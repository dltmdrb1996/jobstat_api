package com.example.jobstat.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.time.Duration

@Configuration
@EnableCaching
class RedisConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.database}") private val dataIndex: Int,
) {
    private val rankingTtl = java.time.Duration.ofDays(32)
    private val defaultTtl = java.time.Duration.ofMinutes(30)

    @Bean
    fun createLettuceConnectionFactory(): RedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.port = port
        redisStandaloneConfiguration.database = dataIndex
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

    @Bean
    fun stringRedisTemplate(): StringRedisTemplate = StringRedisTemplate(createLettuceConnectionFactory())

    @Bean
    fun rankingCacheManager(redisConnectionFactory: RedisConnectionFactory): RedisCacheManager {
        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
                ).serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJackson2JsonRedisSerializer(),
                    ),
                ).computePrefixWith { name -> "ranking:cache:$name:" }

        // 페이징 캐시만 설정
        val cacheConfigurations =
            mapOf(
                "ranking-page" to
                    defaultConfig
                        .entryTtl(java.time.Duration.ofDays(32)) // 5분 TTL
                        .computePrefixWith { "ranking:page:" },
            )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
