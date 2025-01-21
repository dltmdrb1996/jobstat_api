package com.example.jobstat.core.security

import ApiResponse
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.error.StructuredLogger
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtTokenFilter(
    private val jwtTokenParser: JwtTokenParser,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    private val log = StructuredLogger(this::class.java)

    companion object {
        val PERMIT_URLS: Array<String> =
            arrayOf(
                "/api/v1/auth/signup",
                "/api/v1/auth/refresh",
                "/api/v1/auth/signin",
                "/error",
                "/actuator/health",
            )
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = PERMIT_URLS.any { request.requestURI.startsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            getJwtFromRequest(request)?.let { jwt ->
                val accessPayload = jwtTokenParser.validateToken(jwt)
                val authentication = UsernamePasswordAuthenticationToken(accessPayload.id, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            } ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "JWT 토큰이 없습니다")
        } catch (ex: AppException) {
            sendErrorResponse(response, ex)
            return
        } catch (ex: Exception) {
            log.error("JWT 토큰 검증 중 오류 발생", ex)
            sendErrorResponse(
                response,
                AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "JWT 토큰 검증 중 오류가 발생했습니다"),
            )
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? = request.getHeader("Authorization")?.takeIf { it.startsWith(BEARER_PREFIX) }?.substring(BEARER_PREFIX.length)

    private fun sendErrorResponse(
        response: HttpServletResponse,
        appException: AppException,
    ) {
        response.apply {
            characterEncoding = "UTF-8"
            status = appException.httpStatus.value()
            contentType = "application/json; charset=UTF-8"
            writer.write(createErrorJsonResponse(appException))
        }
    }

    private fun createErrorJsonResponse(appException: AppException): String =
        objectMapper.writeValueAsString(
            ApiResponse.fail(
                appException.httpStatus,
                appException.detailInfo().toString(),
            ),
        )
}
