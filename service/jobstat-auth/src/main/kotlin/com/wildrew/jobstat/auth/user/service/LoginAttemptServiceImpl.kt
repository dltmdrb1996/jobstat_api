package com.wildrew.jobstat.auth.user.service

import com.wildrew.jobstat.auth.user.UserConstants
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LoginAttemptServiceImpl(
    cacheManager: CacheManager,
) : LoginAttemptService {
    val cache: Cache =
        cacheManager.getCache(UserConstants.LOGIN_ATTEMPTS_CACHE_NAME)
            ?: throw IllegalStateException("Cache '${UserConstants.LOGIN_ATTEMPTS_CACHE_NAME}' not found")

    override fun incrementFailedAttempts(username: String) {
        getAttempts(username).let { attempts ->
            LoginAttemptInfo(
                count = attempts.count + 1,
                lastFailedAt = Instant.now(),
                blockedUntil = calculateBlockedUntil(attempts.count + 1),
            ).also {
                cache.put(username, it)
            }
        }
    }

    private fun calculateBlockedUntil(failCount: Int): Instant? =
        if (failCount >= UserConstants.MAX_LOGIN_ATTEMPTS) {
            Instant.now().plusSeconds(UserConstants.LOGIN_LOCK_DURATION_MINUTES.toLong() * 60)
        } else {
            null
        }

    override fun isAccountLocked(username: String): Boolean =
        getAttempts(username).let { attempts ->
            when {
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
