package com.example.jobstat.auth.user

import com.example.jobstat.auth.user.usecase.Register
import com.example.jobstat.core.core_web_util.constant.RestConstants
import com.example.jobstat.core.core_web_util.ApiResponse
import com.example.jobstat.core.core_security.annotation.Public
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
@RequestMapping("/api/${RestConstants.Versions.V1}/user")
@Tag(name = "사용자 관리", description = "사용자 계정 등록, 수정, 삭제 등을 관리하는 API")
internal class UserController(
    private val register: Register,
) {
    @Public
    @PostMapping("/register")
    @Operation(
        summary = "회원 가입",
        description = "새로운 사용자 계정을 등록합니다.",
        tags = ["사용자 관리"],
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "사용자 계정이 성공적으로 생성되었습니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "요청 형식이 올바르지 않거나 유효성 검증에 실패했습니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    @SwaggerResponse(
        responseCode = "409",
        description = "이미 존재하는 이메일 또는 사용자명입니다.",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
    )
    fun register(
        @RequestBody request: Register.Request,
    ): ResponseEntity<ApiResponse<Register.Response>> = ApiResponse.ok(register.invoke(request))
}
