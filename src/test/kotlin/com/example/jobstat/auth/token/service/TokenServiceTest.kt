package com.example.jobstat.auth.token.service

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
        @DisplayName("리프레시 토큰 저장에 성공한다")
        fun storeRefreshTokenSuccessfully() {
            val refreshToken = "refresh_token"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)

            val storedToken = stringRedisTemplate.opsForValue().get("refresh_token:$userId")
            assertEquals(refreshToken, storedToken)
        }

        @Test
        @DisplayName("기존 토큰을 새로운 토큰으로 덮어쓰기에 성공한다")
        fun overwriteExistingTokenSuccessfully() {
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
        @DisplayName("토큰의 만료 시간이 올바르게 설정된다")
        fun verifyTokenExpirationTime() {
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
        @DisplayName("유효한 리프레시 토큰으로 사용자 ID 조회에 성공한다")
        fun retrieveUserIdWithValidRefreshToken() {
            val refreshToken = "valid_token"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            val retrievedUserId = tokenService.validateRefreshTokenAndReturnUserId(refreshToken)

            assertEquals(userId, retrievedUserId)
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 조회시 예외가 발생한다")
        fun throwExceptionForNonExistentToken() {
            val invalidToken = "유효하지_않은_토큰"

            assertThrows<AppException> {
                tokenService.validateRefreshTokenAndReturnUserId(invalidToken)
            }.also { exception ->
                assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            }
        }

        @Test
        @DisplayName("만료된 토큰으로 조회시 예외가 발생한다")
        fun throwExceptionForExpiredToken() {
            val refreshToken = "만료된_토큰"
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
        @DisplayName("토큰 삭제에 성공한다")
        fun deleteTokenSuccessfully() {
            val refreshToken = "token_to_delete"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            tokenService.removeToken(userId)

            assertNull(stringRedisTemplate.opsForValue().get("refresh_token:$userId"))
        }

        @Test
        @DisplayName("토큰 무효화에 성공한다")
        fun invalidateTokenSuccessfully() {
            val refreshToken = "token_to_invalidate"
            val userId = 1L
            val expiration = 3600L

            tokenService.storeRefreshToken(refreshToken, userId, expiration)
            tokenService.invalidateRefreshToken(refreshToken)

            assertNull(stringRedisTemplate.opsForValue().get("refresh_token:$userId"))
        }

        @Test
        @DisplayName("존재하지 않는 토큰 무효화 시도시 예외가 발생한다")
        fun throwExceptionForInvalidatingNonExistentToken() {
            val nonExistentToken = "non_existent_token"

            assertThrows<AppException> {
                tokenService.invalidateRefreshToken(nonExistentToken)
            }
        }
    }
}
