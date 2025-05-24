package com.wildrew.app.auth.user.service

import com.wildrew.app.utils.TestCacheManager
import com.wildrew.app.utils.TestClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.*

@DisplayName("LoginAttemptService 테스트")
class LoginAttemptServiceTest {
    private lateinit var loginAttemptService: LoginAttemptServiceImpl
    private lateinit var testClock: TestClock
    private lateinit var cacheManager: TestCacheManager

    @BeforeEach
    fun setUp() {
        testClock = TestClock() // 테스트용 시계 (기본값: now=1970-01-01T00:00...)
        cacheManager = TestCacheManager()
        // LoginAttemptServiceImpl 에 Clock 주입
        loginAttemptService = LoginAttemptServiceImpl(cacheManager)
    }

    @Nested
    @DisplayName("로그인 시도 기록")
    inner class RecordLoginAttempt {
        @Test
        @DisplayName("실패한 로그인 시도를 기록할 수 있다")
        fun recordFailedAttempt() {
            // 실행
            loginAttemptService.incrementFailedAttempts("test@example.com")

            // 검증
            assertFalse(loginAttemptService.isAccountLocked("test@example.com"))
        }

        @Test
        @DisplayName("최대 시도 횟수 초과시 계정이 잠긴다")
        fun blockAfterMaxAttempts() {
            // 실행
            repeat(5) {
                loginAttemptService.incrementFailedAttempts("test@example.com")
            }

            // 검증
            assertTrue(loginAttemptService.isAccountLocked("test@example.com"))
        }

        @Test
        @DisplayName("시도 횟수가 최대값 미만이면 계정이 잠기지 않는다")
        fun noBlockBeforeMaxAttempts() {
            // When
            repeat(4) {
                loginAttemptService.incrementFailedAttempts("test@example.com")
            }

            // Then
            assertFalse(loginAttemptService.isAccountLocked("test@example.com"))
        }
    }

    @Nested
    @DisplayName("시도 횟수 초기화")
    inner class ClearAttempts {
        @Test
        @DisplayName("로그인 시도 기록을 초기화할 수 있다")
        fun clearLoginAttempts() {
            // Given
            repeat(3) {
                loginAttemptService.incrementFailedAttempts("test@example.com")
            }

            // When
            loginAttemptService.resetAttempts("test@example.com")

            // Then
            // 초기화 후 다시 시도 -> 첫 시도처럼 처리
            loginAttemptService.incrementFailedAttempts("test@example.com")
            assertFalse(loginAttemptService.isAccountLocked("test@example.com"))
        }

        @Test
        @DisplayName("잠긴 계정도 초기화할 수 있다")
        fun clearBlockedAccount() {
            // Given
            repeat(5) {
                loginAttemptService.incrementFailedAttempts("test@example.com")
            }
            assertTrue(loginAttemptService.isAccountLocked("test@example.com"))

            // When
            loginAttemptService.resetAttempts("test@example.com")

            // Then
            assertFalse(loginAttemptService.isAccountLocked("test@example.com"))
        }
    }

    @Nested
    @DisplayName("추가 엣지 케이스")
    inner class AdditionalEdgeCases {
        @Test
        @DisplayName("이미 잠긴 계정에 대해 실패 시도를 계속 기록해도 잠김 상태가 유지된다")
        fun recordFailedAttemptWhileBlocked() {
            // Given
            repeat(5) {
                loginAttemptService.incrementFailedAttempts("blocked@example.com")
            }
            assertTrue(loginAttemptService.isAccountLocked("blocked@example.com"))

            // When (이미 blocked 상태에서 다시 실패)
            loginAttemptService.incrementFailedAttempts("blocked@example.com")

            // Then
            // 여전히 잠금
            assertTrue(loginAttemptService.isAccountLocked("blocked@example.com"))
        }

        @Test
        @DisplayName("서로 다른 사용자끼리는 독립적으로 잠금 상태를 가진다")
        fun multipleUsers() {
            // Given
            // 첫 사용자: 5번 실패 -> 잠김
            repeat(5) {
                loginAttemptService.incrementFailedAttempts("user1@example.com")
            }
            // 두 번째 사용자: 2번 실패 -> 잠기지 않음
            repeat(2) {
                loginAttemptService.incrementFailedAttempts("user2@example.com")
            }

            // Then
            assertTrue(loginAttemptService.isAccountLocked("user1@example.com"))
            assertFalse(loginAttemptService.isAccountLocked("user2@example.com"))
        }
    }
}
