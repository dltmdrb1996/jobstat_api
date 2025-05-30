package com.wildrew.jobstat.auth.user.usecase

import com.wildrew.jobstat.auth.token.service.TokenService
import com.wildrew.jobstat.auth.user.UserConstants
import com.wildrew.jobstat.auth.user.service.UserService
import com.wildrew.jobstat.core.core_security.util.PasswordUtil
import com.wildrew.jobstat.core.core_token.JwtTokenGenerator
import com.wildrew.jobstat.core.core_token.model.AccessPayload
import com.wildrew.jobstat.core.core_token.model.RefreshPayload
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class Register(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val passwordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<Register.Request, Register.Response>(validator) {
    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response =
        with(request) {
            // 사용자 생성
            val encodedPassword = passwordUtil.encode(password)
            userService
                .createUser(
                    username = username,
                    email = email,
                    password = encodedPassword,
                    birthDate = birthDate,
                ).let { user ->
                    // 역할 정보 추출 및 토큰 생성
                    val roles = user.getRolesString()
                    val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(user.id, roles))
                    val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(user.id, roles))

                    tokenService.saveToken(refreshToken, user.id, jwtTokenGenerator.getRefreshTokenExpiration())
                    Response(accessToken, refreshToken)
                }
        }

    @Schema(
        name = "RegisterRequest",
        description = "회원 가입 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "사용자명 (영문/한글/숫자 2~20자)",
            example = "홍길동",
            pattern = UserConstants.Patterns.USERNAME_PATTERN,
            required = true,
        )
        @field:NotBlank(message = "사용자명은 필수 입력값입니다")
        @field:Pattern(regexp = UserConstants.Patterns.USERNAME_PATTERN, message = UserConstants.ErrorMessages.INVALID_USERNAME)
        val username: String,
        @field:Schema(
            description = "이메일 주소",
            example = "user@example.com",
            format = "email",
            required = true,
        )
        @field:NotBlank(message = "이메일은 필수 입력값입니다")
        @field:Email(message = "유효한 이메일 형식이 아닙니다")
        @field:Size(max = UserConstants.MAX_EMAIL_LENGTH, message = UserConstants.ErrorMessages.INVALID_EMAIL)
        val email: String,
        @field:Schema(
            description = "비밀번호 (영문 대/소문자, 숫자, 특수문자 조합 8~20자)",
            example = "Password1234!",
            pattern = UserConstants.Patterns.PASSWORD_PATTERN,
            required = true,
        )
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
        @field:Pattern(
            regexp = UserConstants.Patterns.PASSWORD_PATTERN,
            message = UserConstants.ErrorMessages.INVALID_PASSWORD,
        )
        val password: String,
        @field:Schema(
            description = "생년월일",
            example = "1990-01-01",
            format = "date",
            required = true,
        )
        @field:Past(message = UserConstants.ErrorMessages.INVALID_BIRTH_DATE)
        val birthDate: LocalDate,
    )

    @Schema(
        name = "RegisterResponse",
        description = "회원 가입 응답 모델",
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
    )
}
