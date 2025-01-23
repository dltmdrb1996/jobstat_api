package com.example.jobstat.user.service

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.utils.FakeStringRedisTemplate
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class TokenServiceTest {
    private lateinit var stringRedisTemplate: FakeStringRedisTemplate
    private lateinit var tokenService: TokenServiceImpl

    @BeforeEach
    fun setUp() {
        stringRedisTemplate = FakeStringRedisTemplate()
        tokenService = TokenServiceImpl(stringRedisTemplate)
    }

    @Nested
    @DisplayName("토큰 저장")
    inner class StoreToken {
        @Test
        fun `리프레시 토큰 저장 성공`() {
            val refreshToken = "refresh_token"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)

            val storedToken = stringRedisTemplate.opsForValue().get("refresh_token:$userId")
            assertEquals(refreshToken, storedToken)
        }

        @Test
        fun `기존 토큰 덮어쓰기 성공`() {
            val userId = 1L
            val oldToken = "old_token"
            val newToken = "new_token"
            val expiration = 3600L

            tokenService.storeRefreshToken(oldToken, userId, expiration)
            tokenService.storeRefreshToken(newToken, userId, expiration)

            val storedToken = stringRedisTemplate.opsForValue().get("refresh_token:$userId")
            assertEquals(newToken, storedToken)
        }

        @Test
        fun `토큰 만료시간 설정 확인`() {
            val refreshToken = "test_token"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)

            val remainingTime = stringRedisTemplate.getExpire("refresh_token:$userId")
            assertTrue(remainingTime > 0 && remainingTime <= expiration)
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    inner class ValidateToken {
        @Test
        fun `유효한 리프레시 토큰으로 사용자 ID 조회 성공`() {
            val refreshToken = "valid_token"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            val retrievedUserId = tokenService.validateRefreshTokenAndReturnUserId(refreshToken)

            assertEquals(userId, retrievedUserId)
        }

        @Test
        fun `존재하지 않는 토큰으로 조회시 예외 발생`() {
            val invalidToken = "invalid_token"

            assertThrows<AppException> {
                tokenService.validateRefreshTokenAndReturnUserId(invalidToken)
            }.also { exception ->
                assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            }
        }

        @Test
        fun `만료된 토큰으로 조회시 예외 발생`() {
            val refreshToken = "expired_token"
            val userId = 1L
            val expiration = 1L // 1초 후 만료

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            Thread.sleep(1100) // 만료 대기

            assertThrows<AppException> {
                tokenService.validateRefreshTokenAndReturnUserId(refreshToken)
            }
        }
    }

    @Nested
    @DisplayName("토큰 삭제")
    inner class RemoveToken {
        @Test
        fun `토큰 삭제 성공`() {
            val refreshToken = "token_to_delete"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            tokenService.removeToken(userId)

            assertNull(stringRedisTemplate.opsForValue().get("refresh_token:$userId"))
        }

        @Test
        fun `토큰 무효화 성공`() {
            val refreshToken = "token_to_invalidate"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            tokenService.invalidateRefreshToken(refreshToken)

            assertNull(stringRedisTemplate.opsForValue().get("refresh_token:$userId"))
        }

        @Test
        fun `존재하지 않는 토큰 무효화 시도시 예외 발생`() {
            val nonExistentToken = "non_existent_token"

            assertThrows<AppException> {
                tokenService.invalidateRefreshToken(nonExistentToken)
            }
        }
    }
}
