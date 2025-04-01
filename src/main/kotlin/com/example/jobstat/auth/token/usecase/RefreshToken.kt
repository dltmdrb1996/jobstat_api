package com.example.jobstat.auth.token.usecase

import com.example.jobstat.auth.token.service.TokenService
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.security.AccessPayload
import com.example.jobstat.core.security.JwtTokenGenerator
import com.example.jobstat.core.security.RefreshPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Service

@Service
internal class RefreshToken(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    validator: Validator,
) : ValidUseCase<RefreshToken.Request, RefreshToken.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response = with(request) {
        // 리프레시 토큰으로부터 사용자 ID 조회
        tokenService.getUserIdFromToken(refreshToken).let { userId ->
            // 사용자 정보 및 역할 조회
            userService.getUserWithRoles(userId).let { user ->
                // 토큰 페이로드 생성
                val roles = user.getRolesString()
                val refreshPayload = RefreshPayload(user.id, roles)
                val accessPayload = AccessPayload(user.id, roles)
                
                // 새 토큰 생성
                val newRefreshToken = jwtTokenGenerator.createRefreshToken(refreshPayload)
                val newAccessToken = jwtTokenGenerator.createAccessToken(accessPayload)
                
                // 리프레시 토큰 저장
                tokenService.saveToken(newRefreshToken, userId, jwtTokenGenerator.getRefreshTokenExpiration())
                
                // 응답 반환
                Response(newAccessToken, newRefreshToken)
            }
        }
    }

    data class Request(
        @field:NotBlank(message = "리프레시 토큰은 필수 값입니다")
        val refreshToken: String,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
    )
}
