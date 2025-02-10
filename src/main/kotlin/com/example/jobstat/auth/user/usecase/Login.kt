package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.service.LoginAttemptService
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.*
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.RegexPatterns
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
internal class Login(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val bcryptPasswordUtil: PasswordUtil,
    private val loginAttemptService: LoginAttemptService,
    validator: Validator,
) : ValidUseCase<Login.Request, Login.Response>(validator) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun execute(request: Request): Response {
        // 0. 로그인 시도 제한 확인
        if (loginAttemptService.isBlocked(request.email)) {
            throw AppException.fromErrorCode(
                ErrorCode.TOO_MANY_REQUESTS,
                "너무 많은 로그인 시도가 있었습니다. 잠시 후 다시 시도해주세요.",
            )
        }

        // 1. 사용자 조회
        val user =
            try {
                userService.getUserByEmail(request.email)
            } catch (e: Exception) {
                loginAttemptService.recordFailedAttempt(request.email)
                throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    "인증에 실패했습니다",
                )
            }

        // 2. 계정 활성화 상태 확인
        if (!user.isActive) {
            loginAttemptService.recordFailedAttempt(request.email)
            throw AppException.fromErrorCode(
                ErrorCode.ACCOUNT_DISABLED,
                "비활성화된 계정입니다.",
            )
        }

        // 3. 패스워드 검증
        if (!bcryptPasswordUtil.matches(request.password, user.password)) {
            loginAttemptService.recordFailedAttempt(request.email)
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "유저정보가 일치하지 않습니다.",
            )
        }

        // 로그인 성공 시 시도 횟수 초기화
        loginAttemptService.clearAttempts(request.email)

        // 4. 토큰 생성
        val roles = userService.getUserRoles(user.id)
        log.info("사용자 ${user.id}가 다음 권한으로 로그인했습니다: ${roles.joinToString(", ")}")
        val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(user.id, roles))
        val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(user.id, roles))

        // 5. Refresh 토큰 저장
        tokenService.storeRefreshToken(refreshToken, user.id, jwtTokenGenerator.getRefreshTokenExpiration())

        // 6. 응답 생성
        return Response(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.now().plusSeconds(jwtTokenGenerator.getRefreshTokenExpiration()),
            user =
                UserResponse(
                    id = user.id,
                    username = user.username,
                    email = user.email,
                    roles = roles,
                ),
        )
    }

    data class Request(
        @field:Pattern(regexp = RegexPatterns.EMAIL_PATTERN)
        val email: String,
        @field:NotBlank(message = "비밀번호를 입력해주세요.")
        val password: String,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Instant,
        val user: UserResponse,
    )

    data class UserResponse(
        val id: Long,
        val username: String,
        val email: String,
        val roles: List<String>,
    )
}
