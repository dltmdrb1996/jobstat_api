package com.example.jobstat.auth.user.usecase

import com.example.jobstat.core.security.AccessPayload
import com.example.jobstat.core.security.JwtTokenGenerator
import com.example.jobstat.core.security.RefreshPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.RegexPatterns
import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.service.UserService
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.*
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class SignUp(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    validator: Validator,
) : ValidUseCase<SignUp.Request, SignUp.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val user =
            userService.createUser(
                username = request.username,
                email = request.email,
                password = request.password,
                birthDate = request.birthDate,
            )

        val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(user.id, user.getRolesString()))
        val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(user.id, user.getRolesString()))
        tokenService.storeRefreshToken(refreshToken, user.id, jwtTokenGenerator.getRefreshTokenExpiration())
        return Response(accessToken, refreshToken)
    }

    data class Request(
        @field:NotBlank
        @field:Pattern(regexp = RegexPatterns.USERNAME_PATTERN)
        val username: String,

        @field:NotBlank
        @field:Email
        val email: String,

        @field:NotBlank
        @field:Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "Password must be minimum 8 characters, at least one letter, one number and one special character"
        )
        val password: String,

        @field:Past
        val birthDate: LocalDate,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
    )
}