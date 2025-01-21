package com.example.jobstat.user.service

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
internal class TokenServiceImpl(
    private val stringRedisTemplate: StringRedisTemplate,
) : TokenService {
    companion object {
        private const val REFRESH_TOKEN_PREFIX = "refresh_token:"
    }

    override fun storeRefreshToken(
        refreshToken: String,
        userId: Long,
        expirationInSeconds: Long,
    ) {
        val userKey = "$REFRESH_TOKEN_PREFIX$userId"
        // 새 토큰 저장 (기존 토큰이 있다면 자동으로 덮어씁니다)
        stringRedisTemplate.opsForValue().set(userKey, refreshToken, expirationInSeconds, TimeUnit.SECONDS)
    }

    override fun validateRefreshTokenAndReturnUserId(refreshToken: String): Long {
        val userIdKey =
            stringRedisTemplate.keys("$REFRESH_TOKEN_PREFIX*").find { key ->
                stringRedisTemplate.opsForValue().get(key) == refreshToken
            } ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "유효하지 않은 리프레시 토큰입니다.")

        return userIdKey.substringAfter(REFRESH_TOKEN_PREFIX).toLong()
    }

    override fun removeToken(userId: Long) {
        val userKey = "$REFRESH_TOKEN_PREFIX$userId"
        stringRedisTemplate.delete(userKey)
    }

    fun invalidateRefreshToken(refreshToken: String) {
        val userId = validateRefreshTokenAndReturnUserId(refreshToken)
        removeToken(userId)
    }
}
