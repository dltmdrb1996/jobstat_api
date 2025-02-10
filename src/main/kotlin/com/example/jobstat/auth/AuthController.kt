package com.example.jobstat.auth

import com.example.jobstat.auth.token.usecase.RefreshToken
import com.example.jobstat.auth.user.usecase.Login
import com.example.jobstat.auth.user.usecase.Register
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.wrapper.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/auth")
internal class AuthController(
    private val registerUseCase: Register,
    private val loginUseCase: Login,
    private val refreshTokenUseCase: RefreshToken,
) {
    @Public
    @PostMapping("/register")
    fun signUp(
        @RequestBody registerRequest: Register.Request,
    ): ResponseEntity<ApiResponse<Register.Response>> = ApiResponse.ok(registerUseCase(registerRequest))

    @Public
    @PostMapping("/login")
    fun signIn(
        @RequestBody loginRequest: Login.Request,
    ): ResponseEntity<ApiResponse<Login.Response>> = ApiResponse.ok(loginUseCase(loginRequest))

    @Public
    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody refreshTokenRequest: RefreshToken.Request,
    ): ResponseEntity<ApiResponse<RefreshToken.Response>> = ApiResponse.ok(refreshTokenUseCase(refreshTokenRequest))
}
