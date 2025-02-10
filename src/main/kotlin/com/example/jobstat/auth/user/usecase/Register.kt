package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.security.*
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.RegexPatterns
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
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<Register.Request, Register.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val user =
            userService.createUser(
                username = request.username,
                email = request.email,
                password = bcryptPasswordUtil.encode(request.password),
                birthDate = request.birthDate,
            )

        val roles = user.getRolesString()
        val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(user.id, roles))
        val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(user.id, roles))
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
            message = "비밀번호는 최소 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다",
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
