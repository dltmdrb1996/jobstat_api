package com.wildrew.jobstat.auth.token.usecase

import com.wildrew.jobstat.auth.token.service.TokenService
import com.wildrew.jobstat.auth.user.service.UserService
import com.wildrew.jobstat.core.core_token.JwtTokenGenerator
import com.wildrew.jobstat.core.core_token.model.AccessPayload
import com.wildrew.jobstat.core.core_token.model.RefreshPayload
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefreshToken(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    validator: Validator,
) : ValidUseCase<RefreshToken.Request, RefreshToken.Response>(validator) {
    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response =
        with(request) {
            tokenService.getUserIdFromToken(refreshToken).let { userId ->
                userService.getUserWithRoles(userId).let { user ->
                    val roles = user.getRolesString()
                    val refreshPayload = RefreshPayload(user.id, roles)
                    val accessPayload = AccessPayload(user.id, roles)

                    val newRefreshToken = jwtTokenGenerator.createRefreshToken(refreshPayload)
                    val newAccessToken = jwtTokenGenerator.createAccessToken(accessPayload)

                    tokenService.saveToken(newRefreshToken, userId, jwtTokenGenerator.getRefreshTokenExpiration())

                    // 응답 반환
                    Response(newAccessToken, newRefreshToken)
                }
            }
        }

    @Schema(
        name = "RefreshTokenRequest",
        description = "토큰 갱신 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "현재 리프레시 토큰",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            required = true,
        )
        @field:NotBlank(message = "리프레시 토큰은 필수 값입니다")
        val refreshToken: String,
    )

    @Schema(
        name = "RefreshTokenResponse",
        description = "토큰 갱신 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "새로 발급된 액세스 토큰",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        )
        val accessToken: String,
        @field:Schema(
            description = "새로 발급된 리프레시 토큰",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        )
        val refreshToken: String,
    )
}
