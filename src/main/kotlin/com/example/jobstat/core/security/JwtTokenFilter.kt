package com.example.jobstat.core.security

import com.example.jobstat.auth.user.entity.RoleData
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.annotation.AdminAuth
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.security.annotation.PublicWithTokenCheck
import com.example.jobstat.core.wrapper.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.concurrent.TimeUnit

@Component
class JwtTokenFilter(
    private val jwtTokenParser: JwtTokenParser,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    private val requestCache =
        Caffeine
            .newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build<String, RequestCacheData>()

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private val EXCLUDED_PREFIXES = listOf("/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars", "/admin")
        private val EXCLUDED_PATHS = setOf("/swagger-ui.html", "/favicon.ico")

        private val ERROR_MESSAGES =
            mapOf(
                ErrorCode.AUTHENTICATION_FAILURE to "인증 토큰이 필요합니다",
                ErrorCode.ADMIN_ACCESS_REQUIRED to "관리자 권한이 필요합니다",
                ErrorCode.TOKEN_INVALID to "인증 토큰 검증에 실패했습니다",
            )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return when {
            EXCLUDED_PATHS.contains(uri) -> true
            EXCLUDED_PREFIXES.any { uri.startsWith(it) } -> true
            else -> getCachedRequestData(request).shouldNotFilter
        }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (log.isDebugEnabled) {
            log.debug(
                "JWT 필터 처리: {}, shouldNotFilter: {}, cache stats: {}",
                request.requestURI,
                shouldNotFilter(request),
                requestCache.stats(),
            )
        }

        val requestData = getCachedRequestData(request)
        if (requestData.shouldNotFilter) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            getJwtFromRequest(request)?.let { jwt ->
                val accessPayload = jwtTokenParser.validateToken(jwt)
                if (requestData.requiresAdminAuth) {
                    handleAdminAuth(accessPayload)
                } else {
                    setSecurityContextHolder(accessPayload)
                }
            } ?: run {
                if (!requestData.isPublicWithTokenCheck) {
                    sendErrorResponse(response, ErrorCode.AUTHENTICATION_FAILURE)
                    return
                }
            }
        } catch (ex: AppException) {
            sendErrorResponse(response, ex.errorCode)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun getCachedRequestData(request: HttpServletRequest): RequestCacheData {
        val cacheKey = "${request.method}:${request.requestURI}"
        return requestCache.get(cacheKey) { key ->
            val handlerMethod = getHandlerMethod(request)
            RequestCacheData(
                handlerMethod = handlerMethod,
                shouldNotFilter = computeShouldNotFilter(handlerMethod),
                isPublicWithTokenCheck = computeIsPublicWithTokenCheck(handlerMethod),
                requiresAdminAuth = computeRequiresAdminAuth(handlerMethod),
            )
        }
    }

    private fun getHandlerMethod(request: HttpServletRequest): HandlerMethod? = requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod

    private fun computeShouldNotFilter(handlerMethod: HandlerMethod?): Boolean =
        when {
            handlerMethod == null -> false
            handlerMethod.hasMethodAnnotation(Public::class.java) -> true
            handlerMethod.beanType.isAnnotationPresent(Public::class.java) -> true
            else -> false
        }

    private fun computeIsPublicWithTokenCheck(handlerMethod: HandlerMethod?): Boolean =
        when {
            handlerMethod == null -> false
            handlerMethod.hasMethodAnnotation(PublicWithTokenCheck::class.java) -> true
            handlerMethod.beanType.isAnnotationPresent(PublicWithTokenCheck::class.java) -> true
            else -> false
        }

    private fun computeRequiresAdminAuth(handlerMethod: HandlerMethod?): Boolean =
        when {
            handlerMethod == null -> false
            handlerMethod.hasMethodAnnotation(AdminAuth::class.java) -> true
            handlerMethod.beanType.isAnnotationPresent(AdminAuth::class.java) -> true
            else -> false
        }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization") ?: return null
        return if (authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    private fun handleAdminAuth(accessPayload: AccessPayload) {
        if (!accessPayload.roles.contains(RoleData.ADMIN.name)) {
            throw AppException.fromErrorCode(ErrorCode.ADMIN_ACCESS_REQUIRED)
        }
        setSecurityContextHolder(accessPayload)
    }

    private fun setSecurityContextHolder(accessPayload: AccessPayload) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                accessPayload.id,
                null,
                accessPayload.roles.map { SimpleGrantedAuthority("ROLE_$it") },
            )
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        errorCode: ErrorCode,
    ) {
        val apiResponse =
            ApiResponse<Unit>(
                code = errorCode.defaultHttpStatus.value(),
                status = errorCode.defaultHttpStatus,
                message = ERROR_MESSAGES[errorCode] ?: "인증 처리 중 오류가 발생했습니다",
            )

        response.apply {
            characterEncoding = "UTF-8"
            status = errorCode.defaultHttpStatus.value()
            contentType = "application/json; charset=UTF-8"
            writer.write(objectMapper.writeValueAsString(apiResponse))
        }
    }

    data class RequestCacheData(
        val handlerMethod: HandlerMethod?,
        val shouldNotFilter: Boolean,
        val isPublicWithTokenCheck: Boolean,
        val requiresAdminAuth: Boolean,
    )
}
