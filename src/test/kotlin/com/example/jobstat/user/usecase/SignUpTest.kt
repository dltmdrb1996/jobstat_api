package com.example.jobstat.user.usecase

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.security.JwtTokenGenerator
import com.example.jobstat.user.FakeUserRepository
import com.example.jobstat.user.service.TokenService
import com.example.jobstat.user.service.TokenServiceImpl
import com.example.jobstat.user.service.UserService
import com.example.jobstat.user.service.UserServiceImpl
import com.example.jobstat.utils.FakeStringRedisTemplate
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.assertNotNull
import java.time.LocalDate

class SignUpTest {
    private val stringRedisTemplate = FakeStringRedisTemplate()
    private val fakeRepository = FakeUserRepository()
    private val secret = "your-256-bit-secret".repeat(8)
    private val accessExpiration = 3600
    private val refreshExpiration = 86400
    private lateinit var userService: UserService
    private lateinit var tokenService: TokenService
    private lateinit var jwtTokenGenerator: JwtTokenGenerator
    private lateinit var validator: Validator
    private lateinit var signUp: SignUp

    @BeforeEach
    fun setUp() {
        jwtTokenGenerator = JwtTokenGenerator(secret, accessExpiration, refreshExpiration)
        userService = UserServiceImpl(fakeRepository)
        tokenService = TokenServiceImpl(stringRedisTemplate)
        validator = Validation.buildDefaultValidatorFactory().validator
        signUp = SignUp(userService, tokenService, jwtTokenGenerator, validator)
    }

    @Nested
    @DisplayName("회원가입 성공 케이스")
    inner class SignUpSuccess {
        @Test
        @DisplayName("유효한 정보로 회원가입 성공")
        fun signUpWithValidInfo() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20),
                )

            val response = signUp(request)

            assertAll(
                { assertNotNull(response.accessToken) },
                { assertNotNull(response.refreshToken) },
                { assertTrue(stringRedisTemplate.keys("refresh_token:*").isNotEmpty()) },
            )
        }

        @Test
        @DisplayName("토큰이 정상적으로 저장되었는지 확인")
        fun verifyTokenStorage() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20),
                )

            val response = signUp(request)
            val storedToken = stringRedisTemplate.opsForValue().get("refresh_token:1")

            assertEquals(response.refreshToken, storedToken)
        }
    }

    @Nested
    @DisplayName("회원가입 실패 케이스")
    inner class SignUpFailure {
        @Test
        @DisplayName("잘못된 이메일 형식으로 검증 실패")
        fun failWithInvalidEmail() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "invalid-email",
                    birthDate = LocalDate.now().minusYears(20),
                )

            assertThrows<ValidationException> {
                signUp(request)
            }
        }

        @Test
        @DisplayName("미래 생년월일로 검증 실패")
        fun failWithFutureBirthDate() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().plusDays(1),
                )

            assertThrows<ValidationException> {
                signUp(request)
            }
        }

        @Test
        @DisplayName("잘못된 사용자명 패턴으로 검증 실패")
        fun failWithInvalidUsername() {
            val request =
                SignUp.Request(
                    username = "!@#$",
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20),
                )

            assertThrows<ValidationException> {
                signUp(request)
            }
        }

        @Test
        @DisplayName("중복된 이메일로 가입 시도시 실패")
        fun failWithDuplicateEmail() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20),
                )

            signUp(request)

            val duplicateRequest = request.copy(username = "다른유저123")
            assertThrows<AppException> {
                signUp(duplicateRequest)
            }
        }
    }

    @Nested
    @DisplayName("토큰 생성 검증")
    inner class TokenGeneration {
        @Test
        @DisplayName("생성된 토큰이 유효한 JWT 형식인지 확인")
        fun verifyTokenFormat() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20),
                )

            val response = signUp(request)

            assertTrue(response.accessToken.split(".").size == 3)
            assertTrue(response.refreshToken.split(".").size == 3)
        }

        @Test
        @DisplayName("토큰 만료 시간이 정상적으로 설정되었는지 확인")
        fun verifyTokenExpiration() {
            val request =
                SignUp.Request(
                    username = "테스트123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20),
                )

            signUp(request)

            val expiration = stringRedisTemplate.getExpire("refresh_token:1")
            assertTrue(expiration > 0)
            assertTrue(expiration <= refreshExpiration)
        }
    }
}
