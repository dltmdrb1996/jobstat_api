package com.wildrew.app.utils

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCache

class TestCacheManager : CacheManager {
    private val cache = ConcurrentMapCache("loginAttempts")

    override fun getCache(name: String): Cache = cache

    override fun getCacheNames(): Collection<String> = listOf("loginAttempts")
}
