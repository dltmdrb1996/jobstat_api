package com.example.jobstat.auth.token.service

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

    override fun saveToken(
        refreshToken: String,
        userId: Long,
        expirationInSeconds: Long,
    ) {
        // 사용자 키 생성 및 리프레시 토큰 저장
        val userKey = "$REFRESH_TOKEN_PREFIX$userId"
        stringRedisTemplate.opsForValue()
            .set(userKey, refreshToken, expirationInSeconds, TimeUnit.SECONDS)
    }

    override fun getUserIdFromToken(refreshToken: String): Long {
        // 리프레시 토큰으로 사용자 키 찾기
        return findUserKeyByRefreshToken(refreshToken)
            ?.let { userIdKey -> extractUserIdFromKey(userIdKey) }
            ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "유효하지 않은 리프레시 토큰입니다.")
    }

    /**
     * 리프레시 토큰으로 사용자 키를 찾습니다
     */
    private fun findUserKeyByRefreshToken(refreshToken: String): String? =
        stringRedisTemplate.keys("$REFRESH_TOKEN_PREFIX*")
            .find { key -> stringRedisTemplate.opsForValue().get(key) == refreshToken }

    /**
     * 사용자 키에서 사용자 ID를 추출합니다
     */
    private fun extractUserIdFromKey(userIdKey: String): Long = 
        userIdKey.substringAfter(REFRESH_TOKEN_PREFIX).toLong()

    override fun removeToken(userId: Long) {
        val userKey = "$REFRESH_TOKEN_PREFIX$userId"
        stringRedisTemplate.delete(userKey)
    }

    override fun invalidateRefreshToken(refreshToken: String) {
        // 리프레시 토큰으로 사용자 ID를 찾아 토큰 삭제
        getUserIdFromToken(refreshToken).also { userId ->
            removeToken(userId)
        }
    }
}
