package com.wildrew.app.notification

import com.wildrew.app.notification.usecase.RequestEmailVerification
import com.wildrew.app.notification.usecase.VerifyEmail
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_web_util.RestConstants
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/auth/email")
@Tag(name = "이메일 인증", description = "이메일 인증 요청 및 검증 관련 API")
class EmailVerificationController(
    private val requestEmailVerification: RequestEmailVerification,
    private val verifyEmail: VerifyEmail,
) {
    @Public
    @PostMapping("/request")
    @Operation(
        summary = "이메일 인증 요청",
        description = "신규 가입 또는 이메일 변경 시 인증 코드를 담은 이메일을 발송합니다.",
        tags = ["이메일 인증"],
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "인증 이메일이 성공적으로 발송되었습니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "요청 형식이 올바르지 않거나 이미 사용 중인 이메일입니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "429",
        description = "너무 많은 인증 요청이 발생했습니다. 잠시 후 다시 시도해주세요.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    fun requestVerification(
        @RequestBody request: RequestEmailVerification.Request,
    ): ResponseEntity<ApiResponse<Unit>> = ApiResponse.ok(requestEmailVerification(request))

    @Public
    @PostMapping("/verify")
    @Operation(
        summary = "이메일 인증 코드 검증",
        description = "사용자가 이메일로 받은 6자리 인증 코드의 유효성을 검증합니다.",
        tags = ["이메일 인증"],
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "이메일 인증이 성공적으로 완료되었습니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "요청 형식이 올바르지 않거나 인증 코드가 일치하지 않습니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "해당 이메일에 대한 인증 정보를 찾을 수 없습니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "410",
        description = "인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해주세요.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    fun verify(
        @RequestBody request: VerifyEmail.Request,
    ): ResponseEntity<ApiResponse<Unit>> = ApiResponse.ok(verifyEmail(request))
}
