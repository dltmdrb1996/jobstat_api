package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.UserConstants
import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.auth.user.service.LoginAttemptService
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.*
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
internal class Login(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val passwordUtil: PasswordUtil,
    private val loginAttemptService: LoginAttemptService,
    validator: Validator,
) : ValidUseCase<Login.Request, Login.Response>(validator) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response {
        val totalStartTime = Instant.now()

        // 계정 잠금 확인
        checkAccountLock(request.email)

        // 사용자 조회 및 검증
        val user = getUserAndValidate(request.email, request.password)

        // 로그인 시도 초기화
        loginAttemptService.resetAttempts(request.email)

        // 토큰 생성 및 저장
        val (accessToken, refreshToken, expiresAt) = generateAndSaveTokens(user.id, user.getRolesString())

        val totalEndTime = Instant.now()
        log.info("전체 로그인 처리 소요시간: {}ms", Duration.between(totalStartTime, totalEndTime).toMillis())

        // 응답 생성
        return Response(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            user =
                UserResponse(
                    id = user.id.toString(),
                    username = user.username,
                    email = user.email,
                    roles = user.getRolesString(),
                ),
        )
    }

    private fun checkAccountLock(email: String) {
        if (loginAttemptService.isAccountLocked(email)) {
            throw AppException.fromErrorCode(
                ErrorCode.TOO_MANY_REQUESTS,
                UserConstants.ErrorMessages.ACCOUNT_LOCKED,
            )
        }
    }

    private fun getUserAndValidate(
        email: String,
        password: String,
    ): User {
        val getUserStartTime = Instant.now()

        // 사용자 조회
        val user =
            try {
                userService.getUserByEmail(email)
            } catch (e: Exception) {
                loginAttemptService.incrementFailedAttempts(email)
                throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    UserConstants.ErrorMessages.AUTHENTICATION_FAILURE,
                )
            }

        val getUserEndTime = Instant.now()
        log.info("사용자 이메일 조회 소요시간: {}ms", Duration.between(getUserStartTime, getUserEndTime).toMillis())

        // 계정 활성화 확인
        if (!user.isActive) {
            loginAttemptService.incrementFailedAttempts(email)
            throw AppException.fromErrorCode(
                ErrorCode.ACCOUNT_DISABLED,
                UserConstants.ErrorMessages.ACCOUNT_DISABLED,
            )
        }

        // 비밀번호 확인
        val passwordCheckStartTime = Instant.now()
        val passwordMatches = passwordUtil.matches(password, user.password)
        val passwordCheckEndTime = Instant.now()
        log.info("비밀번호 검증 소요시간: {}ms", Duration.between(passwordCheckStartTime, passwordCheckEndTime).toMillis())

        if (!passwordMatches) {
            loginAttemptService.incrementFailedAttempts(email)
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "유저정보가 일치하지 않습니다.",
            )
        }

        return user
    }

    private data class TokenInfo(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Instant,
    )

    private fun generateAndSaveTokens(
        userId: Long,
        roles: List<String>,
    ): TokenInfo {
        // 사용자 권한 조회
        val getRolesStartTime = Instant.now()
        val rolesString = roles.joinToString(", ")
        val getRolesEndTime = Instant.now()
        log.info("사용자 권한 조회 소요시간: {}ms", Duration.between(getRolesStartTime, getRolesEndTime).toMillis())
        log.debug("사용자 ${userId}가 다음 권한으로 로그인했습니다: $rolesString")

        // 토큰 생성
        val tokenGenerationStartTime = Instant.now()
        val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(userId, roles))
        val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(userId, roles))
        val tokenGenerationEndTime = Instant.now()
        log.info("토큰 생성 소요시간: {}ms", Duration.between(tokenGenerationStartTime, tokenGenerationEndTime).toMillis())

        // 토큰 저장
        val tokenSaveStartTime = Instant.now()
        val expiration = jwtTokenGenerator.getRefreshTokenExpiration()
        tokenService.saveToken(refreshToken, userId, expiration)
        val tokenSaveEndTime = Instant.now()
        log.info("토큰 저장 소요시간: {}ms", Duration.between(tokenSaveStartTime, tokenSaveEndTime).toMillis())

        return TokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.now().plusSeconds(expiration),
        )
    }

    @Schema(
        name = "LoginRequest",
        description = "로그인 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "이메일 주소",
            example = "user@example.com",
            pattern = UserConstants.Patterns.EMAIL_PATTERN,
            required = true,
        )
        @field:Pattern(regexp = UserConstants.Patterns.EMAIL_PATTERN, message = "유효한 이메일 형식이 아닙니다")
        val email: String,
        @field:Schema(
            description = "비밀번호",
            example = "Password1234!",
            required = true,
        )
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
        val password: String,
    )

    @Schema(
        name = "LoginResponse",
        description = "로그인 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "발급된 액세스 토큰",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        )
        val accessToken: String,
        @field:Schema(
            description = "발급된 리프레시 토큰",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        )
        val refreshToken: String,
        @field:Schema(
            description = "토큰 만료 시간",
            example = "2023-12-31T23:59:59Z",
            format = "date-time",
        )
        val expiresAt: Instant,
        @field:Schema(
            description = "로그인한 사용자 정보",
        )
        val user: UserResponse,
    )

    @Schema(
        name = "LoginUserResponse",
        description = "로그인 사용자 정보 모델",
    )
    data class UserResponse(
        @field:Schema(
            description = "사용자 ID",
            example = "1",
        )
        val id: String,
        @field:Schema(
            description = "사용자명",
            example = "홍길동",
        )
        val username: String,
        @field:Schema(
            description = "이메일 주소",
            example = "user@example.com",
        )
        val email: String,
        @field:Schema(
            description = "사용자 권한 목록",
            example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]",
        )
        val roles: List<String>,
    )
}
