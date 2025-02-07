package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.token.service.TokenServiceImpl
import com.example.jobstat.auth.user.fake.FakeRoleRepository
import com.example.jobstat.auth.user.fake.FakeUserRepository
import com.example.jobstat.auth.user.fake.UserFixture
import com.example.jobstat.auth.user.service.LoginAttemptServiceImpl
import com.example.jobstat.auth.user.service.UserServiceImpl
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.JwtTokenGenerator
import com.example.jobstat.utils.FakePasswordUtil
import com.example.jobstat.utils.FakeStringRedisTemplate
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.Instant
import kotlin.test.*

@DisplayName("SignIn 유스케이스 테스트")
class LoginTest {
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeRoleRepository: FakeRoleRepository
    private lateinit var passwordUtil: FakePasswordUtil
    private lateinit var userService: UserServiceImpl
    private lateinit var stringRedisTemplate: FakeStringRedisTemplate
    private lateinit var tokenService: TokenService
    private lateinit var jwtTokenGenerator: JwtTokenGenerator
    private lateinit var loginAttemptService: LoginAttemptServiceImpl
    private lateinit var login: Login
    private lateinit var cacheManager: ConcurrentMapCacheManager

    @BeforeEach
    fun setUp() {
        fakeUserRepository = FakeUserRepository()
        fakeRoleRepository = FakeRoleRepository()
        passwordUtil = FakePasswordUtil()
        userService = UserServiceImpl(fakeUserRepository, fakeRoleRepository)
        stringRedisTemplate = FakeStringRedisTemplate()
        tokenService = TokenServiceImpl(stringRedisTemplate)
        jwtTokenGenerator =
            JwtTokenGenerator(
                "your-256-bit-secret".repeat(8),
                3600,
                86400,
            )
        cacheManager =
            ConcurrentMapCacheManager().apply {
                setCacheNames(listOf("loginAttempts"))
            }
        loginAttemptService = LoginAttemptServiceImpl(cacheManager)
        login = Login(userService, tokenService, jwtTokenGenerator, passwordUtil, loginAttemptService, Validation.buildDefaultValidatorFactory().validator)

        fakeUserRepository.clear()
    }

    @Nested
    @DisplayName("기본 로그인 테스트")
    inner class BasicLoginTest {
        @Test
        @DisplayName("올바른 인증 정보로 로그인 성공")
        fun successfulSignIn() {
            // 준비
            val user =
                fakeUserRepository.save(
                    UserFixture
                        .aUser()
                        .withUsername("로그인사용자1") // 정규식 허용
                        .withEmail("login1@example.com")
                        .withPassword(passwordUtil.encode("testpassword123!"))
                        .create(),
                )

            // 실행
            val response = login(Login.Request(user.email, "testpassword123!"))

            // 검증
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
            assertEquals(user.id, response.user.id)
            assertEquals(user.email, response.user.email)
            assertEquals(user.username, response.user.username)
            assertTrue(response.expiresAt.isAfter(Instant.now()))
        }

        @Test
        @DisplayName("이메일 형식이 잘못된 경우 검증 실패")
        fun failWithInvalidEmailFormat() {
            assertFailsWith<ValidationException> {
                login(Login.Request("잘못된-이메일", "testpassword123!"))
            }
        }

        @Test
        @DisplayName("비밀번호가 비어있는 경우 검증 실패")
        fun failWithEmptyPassword() {
            assertFailsWith<ValidationException> {
                login(Login.Request("test@example.com", ""))
            }
        }
    }

    @Nested
    @DisplayName("로그인 실패 케이스")
    inner class LoginFailureTest {
        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시도시 실패")
        fun failWithNonExistentEmail() {
            // when & then
            val exception =
                assertFailsWith<AppException> {
                    login(Login.Request("nonexistent@example.com", "testPassword123!"))
                }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시도시 실패")
        fun failWithWrongPassword() {
            // given
            val user =
                fakeUserRepository.save(
                    UserFixture
                        .aUser()
                        .withEmail("user@example.com")
                        .withPassword("testPassword123!")
                        .create(),
                )

            // when & then
            val exception =
                assertFailsWith<AppException> {
                    login(Login.Request(user.email, "wrongPassword123!"))
                }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
        }

        @Test
        @DisplayName("비활성화된 계정으로 로그인 시도시 실패")
        fun failWithInactiveAccount() {
            // given
            val user =
                fakeUserRepository.save(UserFixture.anInactiveUser()).apply {
                    deactivate()
                }

            // when & then
            val exception =
                assertFailsWith<AppException> {
                    login(Login.Request(user.email, "inactivePass123!"))
                }
            assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.errorCode)
        }
    }

    @Nested
    @DisplayName("로그인 시도 제한 테스트")
    inner class LoginAttemptLimitTest {
        @Test
        @DisplayName("최대 시도 횟수 초과시 계정이 잠김")
        fun accountLockedAfterMaxAttempts() {
            // given
            val user = fakeUserRepository.save(UserFixture.aUser().create())

            // when - 5번 실패
            repeat(5) {
                assertFailsWith<AppException> {
                    login(Login.Request(user.email, "wrongPassword123!"))
                }
            }

            // then - 올바른 비밀번호로도 로그인 불가
            val exception =
                assertFailsWith<AppException> {
                    login(Login.Request(user.email, "testPassword123!"))
                }
            assertEquals(ErrorCode.TOO_MANY_REQUESTS, exception.errorCode)
            assertTrue(loginAttemptService.isBlocked(user.email))
        }

        @Test
        @DisplayName("로그인 성공시 실패 횟수 초기화")
        fun resetAttemptsAfterSuccessfulLogin() {
            // given
            val user =
                fakeUserRepository.save(
                    UserFixture.aUser().withPassword(passwordUtil.encode("testPassword123!")).create(),
                )

            // when - 3번 실패 후 성공
            repeat(3) {
                assertFailsWith<AppException> {
                    login(Login.Request(user.email, "wrongPassword123!"))
                }
            }

            // 성공
            login(Login.Request(user.email, "testPassword123!"))

            // then - 다시 실패해도 바로 잠기진 않음
            assertFailsWith<AppException> {
                login(Login.Request(user.email, "wrongPassword123!"))
            }
            assertFalse(loginAttemptService.isBlocked(user.email))
        }

        @Test
        @DisplayName("실패 기록이 개별 이메일별로 관리됨")
        fun attemptTrackingPerEmail() {
            // given
            val user1 =
                fakeUserRepository.save(
                    UserFixture
                        .aUser()
                        .withUsername("testUser1")
                        .withEmail("user1@example.com")
                        .create(),
                )
            val user2 =
                fakeUserRepository.save(
                    UserFixture
                        .aUser()
                        .withUsername("testUser2")
                        .withPassword(passwordUtil.encode("testPassword123!"))
                        .withEmail("user2@example.com")
                        .create(),
                )

            // when - user1 5번 실패
            repeat(5) {
                assertFailsWith<AppException> {
                    login(Login.Request(user1.email, "wrongPassword123!"))
                }
            }

            // then
            assertTrue(loginAttemptService.isBlocked(user1.email))
            assertFalse(loginAttemptService.isBlocked(user2.email))

            // user2는 여전히 로그인 가능
            val response = login(Login.Request(user2.email, "testPassword123!"))
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }
    }
}
