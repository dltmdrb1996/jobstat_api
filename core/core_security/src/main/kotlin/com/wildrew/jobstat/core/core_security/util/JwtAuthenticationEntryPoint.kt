package com.wildrew.jobstat.core.core_security.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    private val cachedResponse: String by lazy {
        val exception = AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
        val errorResponse =
            ApiResponse<Unit>(
                code = exception.httpStatus.value(),
                status = exception.httpStatus,
                message = "인증이 필요합니다",
            )
        objectMapper.writeValueAsString(errorResponse)
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.apply {
            status = HttpStatus.UNAUTHORIZED.value()
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = "UTF-8"
            writer.write(cachedResponse)
        }
    }
}
