package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.UserConstants
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.security.*
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.*
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class Register(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val passwordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<Register.Request, Register.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response = with(request) {
        // 사용자 생성
        val encodedPassword = passwordUtil.encode(password)
        userService.createUser(
            username = username,
            email = email,
            password = encodedPassword,
            birthDate = birthDate
        ).let { user ->
            // 역할 정보 추출 및 토큰 생성
            val roles = user.getRolesString()
            val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(user.id, roles))
            val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(user.id, roles))
            
            // 토큰 저장 및 응답 생성
            tokenService.saveToken(refreshToken, user.id, jwtTokenGenerator.getRefreshTokenExpiration())
            Response(accessToken, refreshToken)
        }
    }

    data class Request(
        @field:NotBlank
        @field:Pattern(regexp = UserConstants.Patterns.USERNAME_PATTERN, message = UserConstants.ErrorMessages.INVALID_USERNAME)
        val username: String,
        @field:NotBlank
        @field:Email
        @field:Size(max = UserConstants.MAX_EMAIL_LENGTH, message = UserConstants.ErrorMessages.INVALID_EMAIL)
        val email: String,
        @field:NotBlank
        @field:Pattern(
            regexp = UserConstants.Patterns.PASSWORD_PATTERN,
            message = UserConstants.ErrorMessages.INVALID_PASSWORD,
        )
        val password: String,
        @field:Past(message = UserConstants.ErrorMessages.INVALID_BIRTH_DATE)
        val birthDate: LocalDate,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
    )
}
