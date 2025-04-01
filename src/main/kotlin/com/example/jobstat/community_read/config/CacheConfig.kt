package com.example.jobstat.community_read.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration
import java.util.*

/**
 * 캐시 설정 클래스
 */
@Configuration
@EnableCaching
class CacheConfig {
    
    /**
     * Redis 캐시 매니저 설정
     */
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        return RedisCacheManager.builder(connectionFactory)
            .withInitialCacheConfigurations(
                mapOf(
                    "boardViewCount" to RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(1)),
                    "boardLikeCount" to RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(1)),
                    "commentCount" to RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(1))
                )
            )
            .build()
    }
} 