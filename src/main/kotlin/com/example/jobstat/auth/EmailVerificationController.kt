package com.example.jobstat.auth

import com.example.jobstat.auth.email.usecase.RequestEmailVerification
import com.example.jobstat.auth.email.usecase.VerifyEmail
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.wrapper.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/auth/email")
internal class EmailVerificationController(
    private val requestEmailVerification: RequestEmailVerification,
    private val verifyEmail: VerifyEmail,
) {
    @Public
    @PostMapping("/request")
    fun requestVerification(
        @RequestBody request: RequestEmailVerification.Request,
    ): ResponseEntity<ApiResponse<Unit>> = ApiResponse.ok(requestEmailVerification(request))

    @Public
    @PostMapping("/verify")
    fun verify(
        @RequestBody request: VerifyEmail.Request,
    ): ResponseEntity<ApiResponse<Unit>> = ApiResponse.ok(verifyEmail(request))
}
