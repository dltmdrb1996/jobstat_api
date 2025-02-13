package com.example.jobstat.auth

import com.example.jobstat.auth.email.usecase.RequestEmailVerification
import com.example.jobstat.auth.email.usecase.VerifyEmail
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.wrapper.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/auth/email")
@Tag(name = "이메일 인증", description = "이메일 인증 요청 및 검증 관련 API")
internal class EmailVerificationController(
    private val requestEmailVerification: RequestEmailVerification,
    private val verifyEmail: VerifyEmail,
) {
    @Public
    @PostMapping("/request")
    @Operation(summary = "이메일 인증 요청", description = "인증 이메일을 발송합니다.")
    fun requestVerification(
        @RequestBody request: RequestEmailVerification.Request,
    ): ResponseEntity<ApiResponse<Unit>> = ApiResponse.ok(requestEmailVerification(request))

    @Public
    @PostMapping("/verify")
    @Operation(summary = "이메일 인증", description = "사용자가 받은 인증 코드를 검증합니다.")
    fun verify(
        @RequestBody request: VerifyEmail.Request,
    ): ResponseEntity<ApiResponse<Unit>> = ApiResponse.ok(verifyEmail(request))
}
