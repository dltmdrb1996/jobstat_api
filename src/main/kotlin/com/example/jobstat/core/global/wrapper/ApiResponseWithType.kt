package com.example.jobstat.core.global.wrapper

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus

@Schema(description = "API 응답 래퍼 타입")
class ApiResponseWithType<T> {
    @Schema(description = "응답 코드", example = "200")
    val code: Int = 0

    @Schema(description = "HTTP 상태", example = "OK")
    val status: HttpStatus? = null

    @Schema(description = "응답 메시지", example = "Success")
    val message: String = ""

    @JsonUnwrapped
    @Schema(description = "실제 데이터")
    val data: T? = null
}
