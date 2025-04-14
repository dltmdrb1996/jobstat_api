package com.example.jobstat.auth

import com.example.jobstat.auth.token.usecase.RefreshToken
import com.example.jobstat.auth.user.usecase.Login
import com.example.jobstat.auth.user.usecase.Register
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.global.wrapper.ApiResponse
import com.example.jobstat.core.security.annotation.Public
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/auth")
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 갱신 관련 API")
internal class AuthController(
    private val registerUseCase: Register,
    private val loginUseCase: Login,
    private val refreshTokenUseCase: RefreshToken,
) {
    @Public
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    fun signUp(
        @RequestBody registerRequest: Register.Request,
    ): ResponseEntity<ApiResponse<Register.Response>> = ApiResponse.ok(registerUseCase(registerRequest))

    @Public
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 인증 후 토큰을 발급합니다.")
    fun signIn(
        @RequestBody loginRequest: Login.Request,
    ): ResponseEntity<ApiResponse<Login.Response>> = ApiResponse.ok(loginUseCase(loginRequest))

    @Public
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "만료된 토큰을 갱신합니다.")
    fun refreshToken(
        @RequestBody refreshTokenRequest: RefreshToken.Request,
    ): ResponseEntity<ApiResponse<RefreshToken.Response>> = ApiResponse.ok(refreshTokenUseCase(refreshTokenRequest))
}
