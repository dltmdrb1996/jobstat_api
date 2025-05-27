package com.wildrew.jobstat.statistics_read.core

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@EnableCaching
@Configuration
class CacheConfig : CachingConfigurer {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        const val STATS_CACHE_SIZE = 50000L
        const val RANKING_CACHE_SIZE = 100L
        val EXPIRE_AFTER_ACCESS = Duration.ofDays(1)

        // 캐시 이름 상수화
        const val STATS_DOCUMENT_CACHE = "StatsDocument"
        const val STATS_WITH_RANKING_CACHE = "statsWithRanking"
    }

    @Bean
    override fun cacheManager(): CacheManager {
        // SimpleCacheManager 생성
        val simpleCacheManager = SimpleCacheManager()

        // 캐시 목록 생성
        val caches = mutableListOf<org.springframework.cache.Cache>()

        // statsWithRanking - 랭킹 캐싱
        caches.add(
            CaffeineCache(
                STATS_WITH_RANKING_CACHE,
                Caffeine
                    .newBuilder()
                    .maximumSize(RANKING_CACHE_SIZE)
                    .expireAfterAccess(EXPIRE_AFTER_ACCESS)
                    .recordStats()
                    .build(),
            ),
        )

        simpleCacheManager.setCaches(caches)

        return simpleCacheManager
    }
}
