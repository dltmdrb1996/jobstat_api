package com.example.jobstat.auth.user.service

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class LoginAttemptServiceImpl(
    cacheManager: CacheManager,
) : LoginAttemptService {
    val cache: Cache = cacheManager.getCache("loginAttempts")
        ?: throw IllegalStateException("Cache 'loginAttempts' not found")

    companion object {
        private const val MAX_ATTEMPTS = 5
        private val BLOCK_DURATION = Duration.ofMinutes(30)
    }

    override fun recordFailedAttempt(username: String) {
        val attempts = getAttempts(username)
        val attemptInfo = LoginAttemptInfo(
            count = attempts.count + 1,
            lastFailedAt = Instant.now(),
            blockedUntil = if (attempts.count + 1 >= MAX_ATTEMPTS) {
                Instant.now().plus(BLOCK_DURATION)
            } else null
        )
        cache.put(username, attemptInfo)
    }

    override fun isBlocked(username: String): Boolean {
        val attempts = getAttempts(username)
        return when {
            attempts.blockedUntil == null -> false
            attempts.blockedUntil.isBefore(Instant.now()) -> {
                clearAttempts(username)
                false
            }
            else -> true
        }
    }

    override fun clearAttempts(username: String) {
        cache.evict(username)
    }

    private fun getAttempts(username: String): LoginAttemptInfo =
        cache.get(username, LoginAttemptInfo::class.java)
            ?: LoginAttemptInfo(0, null, null)

    data class LoginAttemptInfo(
        val count: Int,
        val lastFailedAt: Instant?,
        val blockedUntil: Instant?
    )
}