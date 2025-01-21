package com.example.jobstat.user.usecase

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.extension.trueOrThrow
import com.example.jobstat.core.security.AccessPayload
import com.example.jobstat.core.security.JwtTokenGenerator
import com.example.jobstat.core.security.RefreshPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.user.service.TokenService
import com.example.jobstat.user.service.UserService
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
    override fun execute(request: Request): Response {
        val id = tokenService.validateRefreshTokenAndReturnUserId(request.refreshToken)
        userService.isActivated(id).trueOrThrow { throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "유효하지 않은 사용자입니다.")}
        val refreshToken = jwtTokenGenerator.createRefreshToken(RefreshPayload(id))
        val accessToken = jwtTokenGenerator.createAccessToken(AccessPayload(id))
        tokenService.storeRefreshToken(refreshToken, id, jwtTokenGenerator.getRefreshTokenExpiration())
        return Response(accessToken, refreshToken)
    }

    data class Request(
        @field:NotBlank
        val refreshToken: String,
    )

    data class Response(
        val accessToken : String,
        val refreshToken : String,
    )
}
