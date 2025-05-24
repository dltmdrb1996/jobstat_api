package com.wildrew.app.auth.user.usecase

import com.wildrew.app.auth.token.service.TokenService
import com.wildrew.app.auth.token.service.TokenServiceImpl
import com.wildrew.app.auth.user.fake.FakeRoleRepository
import com.wildrew.app.auth.user.fake.FakeUserRepository
import com.wildrew.app.auth.user.service.UserService
import com.wildrew.app.auth.user.service.UserServiceImpl
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_token.JwtTokenGenerator
import com.wildrew.app.utils.FakePasswordUtil
import com.wildrew.app.utils.FakeStringRedisTemplate
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("SignUp 유스케이스 테스트")
class RegisterTest {
    private val stringRedisTemplate = FakeStringRedisTemplate()
    private val fakeUserRepository = FakeUserRepository()
    private val fakeRoleRepository = FakeRoleRepository()
    private val passwordUtil = FakePasswordUtil()
    private val secret = "your-256-bit-secret".repeat(8)
    private val accessExpiration = 3600
    private val refreshExpiration = 86400

    private lateinit var userService: UserService
    private lateinit var tokenService: TokenService
    private lateinit var jwtTokenGenerator: JwtTokenGenerator
    private lateinit var register: Register

    @BeforeEach
    fun setUp() {
        jwtTokenGenerator = JwtTokenGenerator(secret, accessExpiration, refreshExpiration)
        userService = UserServiceImpl(fakeUserRepository, fakeRoleRepository)
        tokenService = TokenServiceImpl(stringRedisTemplate)
        val validator = Validation.buildDefaultValidatorFactory().validator
        register = Register(userService, tokenService, jwtTokenGenerator, passwordUtil, validator)

        fakeUserRepository.clear()
    }

    @Nested
    @DisplayName("회원가입 성공 케이스")
    inner class RegisterSuccess {
        @Test
        @DisplayName("유효한 정보로 회원가입 성공")
        fun signUpWithValidInfo() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            val response = register(request)

            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
            assertTrue(stringRedisTemplate.keys("refresh_token:*").isNotEmpty())

            // 패스워드 암호화 확인
            val savedUser = fakeUserRepository.findByEmail(request.email)
            assertTrue(passwordUtil.matches(request.password, savedUser.password))

            // 기본 사용자 역할 부여 확인
            assertTrue(savedUser.hasRole("USER"))
            assertFalse(savedUser.hasRole("ADMIN"))
        }

        @Test
        @DisplayName("토큰이 정상적으로 저장되었는지 확인")
        fun verifyTokenStorage() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            val response = register(request)
            val user = fakeUserRepository.findByEmail(request.email)
            val storedToken = stringRedisTemplate.opsForValue().get("refresh_token:${user.id}")

            assertEquals(response.refreshToken, storedToken)
        }
    }

    @Nested
    @DisplayName("회원가입 실패 케이스")
    inner class RegisterFailure {
        @Test
        @DisplayName("잘못된 이메일 형식으로 검증 실패")
        fun failWithInvalidEmail() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "invalid-email",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            assertFailsWith<ValidationException> {
                register(request)
            }
        }

        @Test
        @DisplayName("약한 패스워드로 검증 실패")
        fun failWithInvalidPassword() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "약함", // 8자 미만, 특수문자/숫자 부족
                    birthDate = LocalDate.now().minusYears(20),
                )

            assertFailsWith<ValidationException> {
                register(request)
            }
        }

        @Test
        @DisplayName("미래 생년월일로 검증 실패")
        fun failWithFutureBirthDate() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().plusDays(1),
                )

            assertFailsWith<ValidationException> {
                register(request)
            }
        }

        @Test
        @DisplayName("잘못된 사용자명 패턴(특수문자만)으로 검증 실패")
        fun failWithInvalidUsername() {
            val request =
                Register.Request(
                    username = "!@#$",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            assertFailsWith<ValidationException> {
                register(request)
            }
        }

        @Test
        @DisplayName("중복된 이메일로 가입 시도시 실패")
        fun failWithDuplicateEmail() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            register(request)

            val duplicateRequest = request.copy(username = "다른유저123")
            val ex =
                assertFailsWith<AppException> {
                    register(duplicateRequest)
                }
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.errorCode)
        }

        @Test
        @DisplayName("중복된 사용자명으로 가입 시도시 실패")
        fun failWithDuplicateUsername() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test1@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            register(request)

            val duplicateRequest = request.copy(email = "test2@example.com")
            val ex =
                assertFailsWith<AppException> {
                    register(duplicateRequest)
                }
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.errorCode)
        }
    }

    @Nested
    @DisplayName("토큰 생성 검증")
    inner class TokenGeneration {
        @Test
        @DisplayName("생성된 토큰이 유효한 JWT 형식인지 확인")
        fun verifyTokenFormat() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            val response = register(request)

            assertTrue(response.accessToken.split(".").size == 3)
            assertTrue(response.refreshToken.split(".").size == 3)
        }

        @Test
        @DisplayName("토큰 만료 시간이 정상적으로 설정되었는지 확인")
        fun verifyTokenExpiration() {
            val request =
                Register.Request(
                    username = "테스트사용자123",
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            register(request)
            val user = fakeUserRepository.findByEmail(request.email)

            val expiration = stringRedisTemplate.getExpire("refresh_token:${user.id}")
            assertTrue(expiration > 0)
            assertTrue(expiration <= refreshExpiration)
        }
    }
}
