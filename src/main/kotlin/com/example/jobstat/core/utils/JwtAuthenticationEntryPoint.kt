package com.example.jobstat.core.utils

import ApiResponse
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
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
