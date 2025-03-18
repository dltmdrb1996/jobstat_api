package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.UserConstants
import com.example.jobstat.auth.user.service.LoginAttemptService
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.*
import com.example.jobstat.core.usecase.impl.ValidUseCase
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
//        if (loginAttemptService.isAccountLocked(request.email)) {
//            throw AppException.fromErrorCode(
//                ErrorCode.TOO_MANY_REQUESTS,
//                UserConstants.ErrorMessages.ACCOUNT_LOCKED,
//            )
//        }

        val user =
            try {
                userService.getUserByEmail(request.email)
            } catch (e: Exception) {
//                loginAttemptService.incrementFailedAttempts(request.email)
                throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    UserConstants.ErrorMessages.AUTHENTICATION_FAILURE,
                )
            }

        if (!user.isActive) {
//            loginAttemptService.incrementFailedAttempts(request.email)
            throw AppException.fromErrorCode(
                ErrorCode.ACCOUNT_DISABLED,
                UserConstants.ErrorMessages.ACCOUNT_DISABLED,
            )
        }

        if (!bcryptPasswordUtil.matches(request.password, user.password)) {
//            loginAttemptService.incrementFailedAttempts(request.email)
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "유저정보가 일치하지 않습니다.",
            )
        }

//        loginAttemptService.resetAttempts(request.email)

        val roles = userService.getUserRoles(user.id)
        log.debug("사용자 ${user.id}가 다음 권한으로 로그인했습니다: ${roles.joinToString(", ")}")
        val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(user.id, roles))
        val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(user.id, roles))

        tokenService.saveToken(refreshToken, user.id, jwtTokenGenerator.getRefreshTokenExpiration())

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
        @field:Pattern(regexp = UserConstants.Patterns.EMAIL_PATTERN)
        val email: String,
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
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
