package com.example.jobstat.user

import ApiResponse
import com.example.jobstat.user.usecase.RefreshToken
import com.example.jobstat.user.usecase.SignUp
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
internal class AuthController(
    private val signUpUseCase: SignUp,
    private val refreshTokenUseCase: RefreshToken,
) {
    @PostMapping("/signup")
    fun signUp(
        @RequestBody signUpRequest: SignUp.Request,
    ): ResponseEntity<ApiResponse<SignUp.Response>> = ApiResponse.ok(signUpUseCase(signUpRequest))

//    @PostMapping("/signin")
//    fun signIn(@RequestBody signInRequest: SignInRequest): ResponseEntity<ApiResponse<SignInResponse>> {
//        val authResponse = authService.signIn(signInRequest)
//        return ResponseEntity.ok(authResponse)
//    }
//
    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody refreshTokenRequest: RefreshToken.Request,
    ): ResponseEntity<ApiResponse<RefreshToken.Response>> = ApiResponse.ok(refreshTokenUseCase(refreshTokenRequest))
}
