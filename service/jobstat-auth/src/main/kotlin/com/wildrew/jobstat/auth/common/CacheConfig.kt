package com.wildrew.jobstat.auth.common

import com.github.benmanes.caffeine.cache.Caffeine
import com.wildrew.jobstat.auth.user.UserConstants.LOGIN_LOCK_DURATION_MINUTES
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
        const val LOGIN_ATTEMPTS_CACHE_SIZE = 10000L
        val LOGIN_ATTEMPT_EXPIRE = Duration.ofMinutes(LOGIN_LOCK_DURATION_MINUTES.toLong())
        val EXPIRE_AFTER_ACCESS = Duration.ofDays(1)

        const val LOGIN_ATTEMPTS_CACHE = "loginAttempts"
    }

    @Bean
    override fun cacheManager(): CacheManager {
        val simpleCacheManager = SimpleCacheManager()

        val caches = mutableListOf<org.springframework.cache.Cache>()

        caches.add(
            CaffeineCache(
                LOGIN_ATTEMPTS_CACHE,
                Caffeine
                    .newBuilder()
                    .maximumSize(LOGIN_ATTEMPTS_CACHE_SIZE)
                    .expireAfterWrite(LOGIN_ATTEMPT_EXPIRE)
                    .recordStats()
                    .build(),
            ),
        )

        simpleCacheManager.setCaches(caches)

        return simpleCacheManager
    }
}
