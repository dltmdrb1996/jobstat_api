package com.wildrew.jobstat.core.core_security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.annotation.AdminAuth
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_security.annotation.PublicWithTokenCheck
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder // ThreadLocal 기반
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.concurrent.TimeUnit

class ThreadLocalGatewayHeaderAuthenticationFilter(
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
        const val HEADER_USER_ID = "X-User-Id"
        const val HEADER_USER_ROLES = "X-User-Roles"
        // EXCLUDED_PREFIXES, EXCLUDED_PATHS, ERROR_MESSAGES는 ScopedValue 버전과 동일하게 사용
         private val EXCLUDED_PREFIXES = listOf(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/admin",
            "/actuator/health",
            "/actuator/prometheus",
            "/actuator/info"
        )
        private val EXCLUDED_PATHS = setOf("/swagger-ui.html", "/favicon.ico")

        private val ERROR_MESSAGES =
            mapOf(
                ErrorCode.AUTHENTICATION_FAILURE to "인증 정보가 필요합니다 (From Gateway)",
                ErrorCode.ADMIN_ACCESS_REQUIRED to "관리자 권한이 필요합니다",
            )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        if (EXCLUDED_PATHS.contains(uri) || EXCLUDED_PREFIXES.any { uri.startsWith(it) }) {
            return true
        }
        return getCachedRequestData(request).isPublicForFiltering
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // SecurityContextHolder 초기화 (ThreadLocal 사용 시 중요)
        SecurityContextHolder.clearContext() // 요청 시작 시 컨텍스트 클리어

        try {
            if (log.isDebugEnabled) {
                log.debug(
                    "Gateway Header Auth Filter 처리 (ThreadLocal): {}, shouldNotFilter(path based + @Public): {}",
                    request.requestURI,
                    shouldNotFilter(request)
                )
            }

            if (shouldNotFilter(request)) {
                filterChain.doFilter(request, response)
                return
            }

            val requestData = getCachedRequestData(request)
            val userIdHeader = request.getHeader(HEADER_USER_ID)
            val userRolesHeader = request.getHeader(HEADER_USER_ROLES)

            var proceed = false

            if (userIdHeader != null && userRolesHeader != null) {
                try {
                    val userId = userIdHeader.toLong()
                    val roles = userRolesHeader.split(",")
                        .filter { it.isNotBlank() }
                        .map { SimpleGrantedAuthority("ROLE_$it") }

                    if (requestData.requiresAdminAuth && !roles.any { it.authority == "ROLE_ADMIN" }) {
                        sendErrorResponse(response, ErrorCode.ADMIN_ACCESS_REQUIRED)
                        return // ADMIN 권한 없음
                    }

                    val authentication = UsernamePasswordAuthenticationToken(userId, null, roles)
                    SecurityContextHolder.getContext().authentication = authentication
                    proceed = true
                } catch (e: NumberFormatException) {
                    log.warn("Invalid X-User-Id header: {}", userIdHeader, e)
                    // 인증 실패로 처리
                } catch (e: Exception) {
                     log.error("Error processing gateway headers for ThreadLocal", e)
                     sendErrorResponse(response, ErrorCode.INTERNAL_ERROR)
                     return
                }
            }

            // 헤더를 통해 인증되었거나, 헤더가 없지만 @PublicWithTokenCheck인 경우
            if (proceed) {
                filterChain.doFilter(request, response)
            } else if (requestData.isPublicWithTokenCheck) { // 헤더가 없거나 유효하지 않지만, @PublicWithTokenCheck
                filterChain.doFilter(request, response) // 비인증 상태로 진행
            } else { // 헤더가 없거나 유효하지 않고, 인증이 필요한 경우
                sendErrorResponse(response, ErrorCode.AUTHENTICATION_FAILURE)
            }
        } finally {
            // 요청 처리 후 SecurityContextHolder 클리어 (ThreadLocal 누수 방지)
            SecurityContextHolder.clearContext()
        }
    }

    // getCachedRequestData, getHandlerMethod, computeIsPublicAnnotation,
    // computeIsPublicWithTokenCheck, computeRequiresAdminAuth, sendErrorResponse 메서드는
    // ScopedValue 버전과 거의 동일하게 구현합니다.
    private fun getCachedRequestData(request: HttpServletRequest): RequestCacheData {
        val cacheKey = "${request.method}:${request.requestURI}"
        return requestCache.get(cacheKey) { _ ->
            val handlerMethod = getHandlerMethod(request)
            RequestCacheData(
                handlerMethod = handlerMethod,
                isPublicForFiltering = computeIsPublicAnnotation(handlerMethod),
                isPublicWithTokenCheck = computeIsPublicWithTokenCheck(handlerMethod),
                requiresAdminAuth = computeRequiresAdminAuth(handlerMethod),
            )
        }
    }

    private fun getHandlerMethod(request: HttpServletRequest): HandlerMethod? = try {
        requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod
    } catch (e: Exception) {
        log.trace("Could not get handler method for request: {}", request.requestURI, e)
        null
    }


    private fun computeIsPublicAnnotation(handlerMethod: HandlerMethod?): Boolean =
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

    private fun sendErrorResponse(
        response: HttpServletResponse,
        errorCode: ErrorCode,
    ) {
        val apiResponse =
            ApiResponse<Unit>(
                code = errorCode.defaultHttpStatus.value(),
                status = errorCode.defaultHttpStatus,
                message = ERROR_MESSAGES[errorCode] ?: "인증 처리 중 오류가 발생했습니다.",
            )

        response.apply {
            characterEncoding = "UTF-8"
            status = errorCode.defaultHttpStatus.value()
            contentType = "application/json; charset=UTF-8"
            try {
                writer.write(objectMapper.writeValueAsString(apiResponse))
            } catch (e: Exception) {
                log.error("Error writing error response to HttpServletResponse", e)
            }
        }
    }

    // RequestCacheData는 ScopedValue 버전과 동일
    data class RequestCacheData(
        val handlerMethod: HandlerMethod?,
        val isPublicForFiltering: Boolean, // @Public 어노테이션만 고려
        val isPublicWithTokenCheck: Boolean,
        val requiresAdminAuth: Boolean,
    )
}