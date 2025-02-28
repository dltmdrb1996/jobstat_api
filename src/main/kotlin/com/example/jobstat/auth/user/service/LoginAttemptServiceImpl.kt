package com.example.jobstat.auth.user.service

import com.example.jobstat.auth.user.UserConstants
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Instant

@Component
internal class LoginAttemptServiceImpl(
    cacheManager: CacheManager,
) : LoginAttemptService {
    val cache: Cache =
        cacheManager.getCache(UserConstants.LOGIN_ATTEMPTS_CACHE_NAME)
            ?: throw IllegalStateException("Cache '${UserConstants.LOGIN_ATTEMPTS_CACHE_NAME}' not found")

    override fun incrementFailedAttempts(username: String) {
        val attempts = getAttempts(username)
        val attemptInfo =
            LoginAttemptInfo(
                count = attempts.count + 1,
                lastFailedAt = Instant.now(),
                blockedUntil =
                    if (attempts.count + 1 >= UserConstants.MAX_LOGIN_ATTEMPTS) {
                        Instant.now().plusSeconds(UserConstants.LOGIN_LOCK_DURATION_MINUTES.toLong() * 60)
                    } else {
                        null
                    },
            )
        cache.put(username, attemptInfo)
    }

    override fun isAccountLocked(username: String): Boolean {
        val attempts = getAttempts(username)
        return when {
            attempts.blockedUntil == null -> false
            attempts.blockedUntil.isBefore(Instant.now()) -> {
                resetAttempts(username)
                false
            }
            else -> true
        }
    }

    override fun resetAttempts(username: String) {
        cache.evict(username)
    }

    private fun getAttempts(username: String): LoginAttemptInfo =
        cache.get(username, LoginAttemptInfo::class.java)
            ?: LoginAttemptInfo(0, null, null)

    data class LoginAttemptInfo(
        val count: Int,
        val lastFailedAt: Instant?,
        val blockedUntil: Instant?,
    )
}
