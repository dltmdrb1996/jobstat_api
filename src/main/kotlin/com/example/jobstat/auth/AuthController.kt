package com.example.jobstat.auth

import ApiResponse
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.auth.token.usecase.RefreshToken
import com.example.jobstat.auth.user.usecase.SignIn
import com.example.jobstat.auth.user.usecase.SignUp
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/auth")
internal class AuthController(
    private val signUpUseCase: SignUp,
    private val signInUseCase: SignIn,
    private val refreshTokenUseCase: RefreshToken,
) {
    @Public
    @PostMapping("/signup")
    fun signUp(
        @RequestBody signUpRequest: SignUp.Request,
    ): ResponseEntity<ApiResponse<SignUp.Response>> = ApiResponse.ok(signUpUseCase(signUpRequest))

    @Public
    @PostMapping("/signin")
    fun signIn(@RequestBody signInRequest: SignIn.Request): ResponseEntity<ApiResponse<SignIn.Response>> =
        ApiResponse.ok(signInUseCase(signInRequest))

    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody refreshTokenRequest: RefreshToken.Request,
    ): ResponseEntity<ApiResponse<RefreshToken.Response>> = ApiResponse.ok(refreshTokenUseCase(refreshTokenRequest))
}
