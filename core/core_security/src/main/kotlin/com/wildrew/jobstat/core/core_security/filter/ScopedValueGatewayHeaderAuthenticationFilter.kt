package com.wildrew.jobstat.core.core_security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.annotation.AdminAuth
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_security.annotation.PublicWithTokenCheck
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
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.concurrent.TimeUnit

class ScopedValueGatewayHeaderAuthenticationFilter(
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
                ErrorCode.AUTHENTICATION_FAILURE to "인증 정보가 필요합니다 (From Gateway)", // 메시지 약간 수정
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
        if (log.isDebugEnabled) {
            log.debug(
                "Gateway Header Auth Filter 처리 (ScopedValue): {}, shouldNotFilter(path based + @Public): {}",
                request.requestURI,
                shouldNotFilter(request)
            )
        }

        if (shouldNotFilter(request)) { // 경로 기반 제외 또는 @Public 어노테이션
            ScopedSecurityContextHolder.runWithNewContext {
                doFilterAndHandleException(filterChain, request, response)
            }
            return
        }

        val requestData = getCachedRequestData(request)
        val userIdHeader = request.getHeader(HEADER_USER_ID)
        val userRolesHeader = request.getHeader(HEADER_USER_ROLES)
        log.debug(
            "Gateway Header Auth Filter: userIdHeader={}, userRolesHeader={}, requestData={}",
            userIdHeader,
            userRolesHeader,
            requestData
        )
        var isAuthenticatedByHeader = false
        var isAdminAuthorizedByHeader = true // 기본적으로 통과, AdminAuth 시 체크

        if (userIdHeader != null && userRolesHeader != null) {
            try {
                val userId = userIdHeader.toLong()
                val roles = userRolesHeader.split(",")
                    .filter { it.isNotBlank() }
                    .map { SimpleGrantedAuthority("ROLE_$it") }

                if (requestData.requiresAdminAuth && !roles.any { it.authority == "ROLE_ADMIN" }) {
                    isAdminAuthorizedByHeader = false
                }

                if (isAdminAuthorizedByHeader) {
                    val authentication = UsernamePasswordAuthenticationToken(userId, null, roles)
                    val securityContextWithAuth = SecurityContextImpl().apply { this.authentication = authentication }
                    ScopedSecurityContextHolder.runWithContext(securityContextWithAuth) {
                        doFilterAndHandleException(filterChain, request, response)
                    }
                    isAuthenticatedByHeader = true
                }
            } catch (e: NumberFormatException) {
                log.warn("Invalid X-User-Id header: {}", userIdHeader, e)
                // 헤더가 있지만 파싱 실패 시 인증 실패로 간주 가능
            } catch (e: Exception) { // 그 외 예외 (거의 발생 안 함)
                log.error("Error processing gateway headers for ScopedValue", e)
                sendErrorResponse(response, ErrorCode.INTERNAL_ERROR) // 또는 다른 적절한 에러
                return
            }
        }

        // 헤더를 통해 인증/인가 되었거나, 헤더가 없지만 @PublicWithTokenCheck 인 경우
        if (isAuthenticatedByHeader && isAdminAuthorizedByHeader) {
            // 이미 위에서 ScopedSecurityContextHolder.runWithContext로 처리하고 리턴됨
            return
        } else if (!isAdminAuthorizedByHeader) { // AdminAuth 필요하지만 헤더에서 Admin 권한 확인 안됨
            sendErrorResponse(response, ErrorCode.ADMIN_ACCESS_REQUIRED)
        } else if (requestData.isPublicWithTokenCheck) { // 헤더가 없거나 유효하지 않지만, @PublicWithTokenCheck인 경우
            ScopedSecurityContextHolder.runWithNewContext {
                doFilterAndHandleException(filterChain, request, response)
            }
        } else { // 헤더가 없거나 유효하지 않고, @Public 또는 @PublicWithTokenCheck도 아닌 경우 (인증 필요)
            sendErrorResponse(response, ErrorCode.AUTHENTICATION_FAILURE)
        }
    }

    private fun doFilterAndHandleException(filterChain: FilterChain, request: HttpServletRequest, response: HttpServletResponse) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            if (e is RuntimeException) throw e
            else throw RuntimeException(e)
        }
    }


    // getCachedRequestData, getHandlerMethod, computeIsPublicAnnotation,
    // computeIsPublicWithTokenCheck, computeRequiresAdminAuth, sendErrorResponse 메서드는
    // 기존 ScopedValueJwtTokenFilter의 내용을 거의 그대로 사용하거나 약간 수정합니다.
    // getJwtFromRequest, handleAdminAuth, setSecurityContextHolder는 더 이상 필요 없습니다.

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
    } catch (e: Exception) { // Tomcat의 IllegalStateException 등 처리
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

    data class RequestCacheData(
        val handlerMethod: HandlerMethod?,
        val isPublicForFiltering: Boolean,
        val isPublicWithTokenCheck: Boolean,
        val requiresAdminAuth: Boolean,
    )
}