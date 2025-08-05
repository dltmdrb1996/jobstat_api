package com.wildrew.jobstat.core.core_security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class GatewayHeaderAuthenticationFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        const val HEADER_USER_ID = "X-User-Id"
        const val HEADER_USER_ROLES = "X-User-Roles"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userIdHeader = request.getHeader(HEADER_USER_ID)
        val userRolesHeader = request.getHeader(HEADER_USER_ROLES)

        try {
            if (userIdHeader != null && userRolesHeader != null) {
                try {
                    val userId = userIdHeader.toLong()
                    val roles = userRolesHeader.split(",")
                        .filter { it.isNotBlank() }
                        .map { SimpleGrantedAuthority("ROLE_" + it.trim()) }

                    val authentication = UsernamePasswordAuthenticationToken(userId, null, roles)

                    SecurityContextHolder.getContext().authentication = authentication
                    log.info("인증 객체가 SecurityContextHolder에 설정되었습니다: {}", authentication)

                } catch (e: Exception) {
                    log.error("게이트웨이 인증 헤더 처리 실패. userId: {}, roles: {}", userIdHeader, userRolesHeader, e)
                    sendErrorResponse(response, ErrorCode.INTERNAL_ERROR, "Invalid authentication headers from gateway.")
                    return
                }
            } else {
                log.warn("게이트웨이 인증 헤더가 없습니다. 익명 사용자로 진행합니다.")
            }

            filterChain.doFilter(request, response)

        } finally {
            SecurityContextHolder.clearContext()
            log.info("SecurityContext cleared for request: {}", request.requestURI)
        }
    }

    private fun sendErrorResponse(response: HttpServletResponse, errorCode: ErrorCode, message: String) {
        val apiResponse = ApiResponse<Unit>(
            code = errorCode.defaultHttpStatus.value(),
            status = errorCode.defaultHttpStatus,
            message = message,
        )
        response.apply {
            characterEncoding = "UTF-8"
            status = errorCode.defaultHttpStatus.value()
            contentType = "application/json; charset=UTF-8"
            try {
                writer.write(objectMapper.writeValueAsString(apiResponse))
            } catch (e: Exception) {
                log.error("에러 응답 작성 중 오류 발생", e)
            }
        }
    }
}