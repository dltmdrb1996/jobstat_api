package com.example.jobstat.core.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

// @EnableScheduling
@EnableCaching
@Configuration
class CacheConfig : CachingConfigurer {
    private val log = LoggerFactory.getLogger(CacheConfig::class.java) // 로거 추가

    companion object {
        private const val STATS_CACHE_SIZE = 2000L
        private const val RANKING_CACHE_SIZE = 100L
        private val EXPIRE_AFTER_ACCESS = Duration.ofDays(1)
    }

    @Bean
    override fun cacheManager(): CacheManager {
        // 기본 Caffeine 설정
        val defaultCaffeine =
            Caffeine
                .newBuilder()
                .maximumSize(STATS_CACHE_SIZE)
                .expireAfterAccess(EXPIRE_AFTER_ACCESS)
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
                    .expireAfterAccess(EXPIRE_AFTER_ACCESS)
                    .recordStats()
                    .build()

            caches["statsWithRanking"] =
                Caffeine
                    .newBuilder()
                    .maximumSize(RANKING_CACHE_SIZE)
                    .expireAfterAccess(EXPIRE_AFTER_ACCESS)
                    .recordStats()
                    .build()

            val cacheNames = setOf("StatsByEntityIdAndBaseDate", "statsWithRanking")
            setCacheNames(cacheNames)
        }
    }

//    @Scheduled(fixedRate = 1000)
//    fun logCacheStats() {
//        val cacheManager = cacheManager()
//        cacheManager.cacheNames.forEach { cacheName ->
//            val cache = cacheManager.getCache(cacheName)
//            if (cache is CaffeineCache) {
//                val stats = cache.nativeCache.stats()
//                log.info("""
//                    Cache '$cacheName' Stats:
//                    - Hits: ${stats.hitCount()}
//                    - Misses: ${stats.missCount()}
//                    - Hit Rate: ${String.format("%.2f", stats.hitRate() * 100)}%
//                    - Evictions: ${stats.evictionCount()}
//                """.trimIndent())
//            }
//        }
//    }
}
