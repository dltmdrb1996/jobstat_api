package com.example.jobstat.core.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration

@EnableScheduling
@EnableCaching
@Configuration
class CacheConfig : CachingConfigurer {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        private const val STATS_CACHE_SIZE = 50000L
        private const val RANKING_CACHE_SIZE = 100L
        private const val LOGIN_ATTEMPTS_CACHE_SIZE = 10000L
        private val LOGIN_ATTEMPT_EXPIRE = Duration.ofMinutes(30)
        private val EXPIRE_AFTER_ACCESS = Duration.ofDays(1)
    }

    @Bean
    override fun cacheManager(): CacheManager {
        // 기본 Caffeine 설정
        val defaultCaffeine =
            Caffeine
                .newBuilder()
                .maximumSize(STATS_CACHE_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_ACCESS)
                .recordStats()

        return CaffeineCacheManager().apply {
            setCaffeine(defaultCaffeine)
            isAllowNullValues = true

            // 캐시별 설정
            val caches = mutableMapOf<String, com.github.benmanes.caffeine.cache.Cache<Any, Any>>()

            caches["StatsByEntityIdAndBaseDate"] =
                Caffeine
                    .newBuilder()
                    .maximumSize(STATS_CACHE_SIZE)
                    .expireAfterWrite(EXPIRE_AFTER_ACCESS)
                    .recordStats()
                    .build()

            caches["statsWithRanking"] =
                Caffeine
                    .newBuilder()
                    .maximumSize(RANKING_CACHE_SIZE)
                    .expireAfterAccess(EXPIRE_AFTER_ACCESS)
                    .recordStats()
                    .build()

            caches["loginAttempts"] =
                Caffeine
                    .newBuilder()
                    .maximumSize(LOGIN_ATTEMPTS_CACHE_SIZE)
                    .expireAfterWrite(LOGIN_ATTEMPT_EXPIRE)
                    .recordStats()
                    .build()

            val cacheNames = setOf("StatsByEntityIdAndBaseDate", "statsWithRanking", "loginAttempts")
            setCacheNames(cacheNames)
        }
    }

    @Scheduled(fixedRate = 1000)
    fun logCacheStats() {
        val cacheManager = cacheManager()
        cacheManager.cacheNames.forEach { cacheName ->
            val cache = cacheManager.getCache(cacheName)
            if (cache is CaffeineCache) {
                val stats = cache.nativeCache.stats()
                log.info(
                    """
                    캐시 '$cacheName' 통계:
                    - 히트: ${stats.hitCount()}
                    - 미스: ${stats.missCount()}
                    - 히트율: ${String.format("%.2f", stats.hitRate() * 100)}%
                    - 제거: ${stats.evictionCount()}
                    """.trimIndent(),
                )
            }
        }
    }
}
