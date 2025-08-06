package com.wildrew.jobstat.core.core_security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.ScopedSecurityContextHolder
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.filter.OncePerRequestFilter

class ScopedValueGatewayHeaderAuthenticationFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    companion object {
        const val HEADER_USER_ID = "X-User-Id"
        const val HEADER_USER_ROLES = "X-User-Roles"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        log.info("==================== Gateway Auth Filter - START ====================")
        log.info("Request URI: {}", request.requestURI)

        val userIdHeader = request.getHeader(HEADER_USER_ID)
        val userRolesHeader = request.getHeader(HEADER_USER_ROLES)

        log.info("Received Header -> {}: {}", HEADER_USER_ID, userIdHeader)
        log.info("Received Header -> {}: {}", HEADER_USER_ROLES, userRolesHeader)

        if (userIdHeader != null && userRolesHeader != null) {
            log.info(">>> Headers FOUND. Attempting to create Authentication object.")

            try {
                val userId = userIdHeader.toLong()
                val roles =
                    userRolesHeader
                        .split(",")
                        .filter { it.isNotBlank() }
                        .map { SimpleGrantedAuthority("ROLE_" + it.trim()) }

                val authentication = UsernamePasswordAuthenticationToken(userId, null, roles)
                val securityContext =
                    SecurityContextImpl().apply {
                        this.authentication = authentication
                    }

                log.info("Authentication object created successfully: {}", authentication)

                ScopedSecurityContextHolder.runWithContext(securityContext) {
                    filterChain.doFilter(request, response)
                }
            } catch (e: Exception) {
                log.error("게이트웨이 인증 헤더 처리 실패. userId: {}, roles: {}", userIdHeader, userRolesHeader, e)
                sendErrorResponse(response, ErrorCode.INTERNAL_ERROR, "Invalid authentication headers from gateway.")
            }
        } else {
            log.warn(">>> Headers NOT FOUND or incomplete. Passing request as anonymous.")

            ScopedSecurityContextHolder.runWithNewContext {
                filterChain.doFilter(request, response)
            }
        }

        log.info("==================== Gateway Auth Filter - END ======================")
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        errorCode: ErrorCode,
        message: String,
    ) {
        val apiResponse =
            ApiResponse<Unit>(
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
                log.error("HttpServletResponse에 에러 응답을 작성하는 중 오류 발생", e)
            }
        }
    }
}
