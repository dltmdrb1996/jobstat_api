package com.wildrew.jobstat.core.core_security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.annotation.AdminAuth
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_security.annotation.PublicWithTokenCheck
import com.wildrew.jobstat.core.core_security.util.ScopedSecurityContextHolder
import com.wildrew.jobstat.core.core_token.JwtTokenParser
import com.wildrew.jobstat.core.core_token.model.AccessPayload
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

class ScopedValueJwtTokenFilter(
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
        private val EXCLUDED_PREFIXES = listOf("/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars", "/admin", "/actuator/health", "/actuator/prometheus")
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
        // 캐시를 먼저 확인하고, 없으면 계산 후 캐시에 저장.
        // 이 메서드는 필터링을 "하지 말아야 할지" 여부만 결정.
        // 어노테이션 기반(@Public 등) 필터링 여부는 doFilterInternal에서 결정.
        if (EXCLUDED_PATHS.contains(uri) || EXCLUDED_PREFIXES.any { uri.startsWith(it) }) {
            return true
        }
        // 핸들러 메서드에 @Public 어노테이션이 있으면 필터링하지 않음 (토큰 검증 X)
        return getCachedRequestData(request).isPublicForFiltering // isPublicForFiltering: @Public 여부
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // shouldNotFilter에서 true가 반환된 경우는 이미 이 필터를 통과했으므로,
        // 여기서는 어노테이션 기반의 세밀한 제어 및 ScopedValue 컨텍스트 설정을 담당.

        if (log.isDebugEnabled) {
            log.debug(
                "JWT 필터 처리 (ScopedValue): {}, shouldNotFilter(path based + @Public): {}, cache stats: {}",
                request.requestURI,
                shouldNotFilter(request), // 이 값은 경로 및 @Public 까지 고려한 값
                requestCache.stats(),
            )
        }

        // shouldNotFilter(request)가 true이면, @Public이거나 경로 제외된 경우임.
        // 이 경우, 빈 SecurityContext로 ScopedValue를 설정하고 다음 필터 실행.
        if (shouldNotFilter(request)) {
            ScopedSecurityContextHolder.runWithNewContext {
                // Runnable 사용 예시
                try {
                    filterChain.doFilter(request, response)
                } catch (e: Exception) {
                    // 예외를 다시 던지거나 처리. ScopedValue.run은 예외를 전파함.
                    // 여기서는 ServletException, IOException 등을 고려해야 함.
                    if (e is RuntimeException) {
                        throw e
                    } else {
                        throw RuntimeException(e) // 또는 특정 예외로 감싸기
                    }
                }
            }
            return
        }

        val requestData = getCachedRequestData(request) // @PublicWithTokenCheck, @AdminAuth 정보 포함

        try {
            val jwt = getJwtFromRequest(request)
            var accessPayload: AccessPayload? = null

            if (jwt != null) {
                accessPayload = jwtTokenParser.validateToken(jwt)
            }

            if (accessPayload != null) { // 토큰이 있고 유효한 경우
                if (requestData.requiresAdminAuth && !accessPayload.roles.contains("ADMIN")) {
                    sendErrorResponse(response, ErrorCode.ADMIN_ACCESS_REQUIRED)
                    return
                }

                // 인증된 사용자의 SecurityContext 생성
                val authentication =
                    UsernamePasswordAuthenticationToken(
                        accessPayload.id,
                        null,
                        accessPayload.roles.map { SimpleGrantedAuthority("ROLE_$it") },
                    )
                val securityContextWithAuth = SecurityContextImpl()
                securityContextWithAuth.authentication = authentication

                // 인증된 컨텍스트로 다음 필터 실행
                ScopedSecurityContextHolder.runWithContext(securityContextWithAuth) {
                    try {
                        filterChain.doFilter(request, response)
                    } catch (e: Exception) {
                        if (e is RuntimeException) {
                            throw e
                        } else {
                            throw RuntimeException(e)
                        }
                    }
                }
            } else { // 토큰이 없거나 유효하지 않은 경우
                if (requestData.isPublicWithTokenCheck) {
                    // @PublicWithTokenCheck: 토큰 없어도 통과 (비인증 상태)
                    ScopedSecurityContextHolder.runWithNewContext {
                        try {
                            filterChain.doFilter(request, response)
                        } catch (e: Exception) {
                            if (e is RuntimeException) {
                                throw e
                            } else {
                                throw RuntimeException(e)
                            }
                        }
                    }
                } else {
                    sendErrorResponse(response, ErrorCode.AUTHENTICATION_FAILURE)
                    return
                }
            }
        } catch (ex: AppException) {
            sendErrorResponse(response, ex.errorCode)
        } catch (e: Exception) {
            log.error("Unexpected error in JwtTokenFilter: {}", e.message, e)
            sendErrorResponse(response, ErrorCode.INTERNAL_ERROR)
        }
    }

    private fun getCachedRequestData(request: HttpServletRequest): RequestCacheData {
        val cacheKey = "${request.method}:${request.requestURI}"
        return requestCache.get(cacheKey) { _ ->
            val handlerMethod = getHandlerMethod(request)
            RequestCacheData(
                handlerMethod = handlerMethod,
                // shouldNotFilter -> isPublicForFiltering 으로 변경하여 @Public만 판단
                isPublicForFiltering = computeIsPublicAnnotation(handlerMethod),
                isPublicWithTokenCheck = computeIsPublicWithTokenCheck(handlerMethod),
                requiresAdminAuth = computeRequiresAdminAuth(handlerMethod),
            )
        }
    }

    private fun getHandlerMethod(request: HttpServletRequest): HandlerMethod? = requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod

    // computeShouldNotFilter -> computeIsPublicAnnotation 이름 변경 및 역할 명확화
    // 이 함수는 @Public 어노테이션 유무만 판단
    private fun computeIsPublicAnnotation(handlerMethod: HandlerMethod?): Boolean =
        when {
            handlerMethod == null -> false // 핸들러가 없으면 기본적으로 Public이 아님
            handlerMethod.hasMethodAnnotation(Public::class.java) -> true
            handlerMethod.beanType.isAnnotationPresent(Public::class.java) -> true
            else -> false
        }

    // computeIsPublicWithTokenCheck, computeRequiresAdminAuth 는 변경 없음
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
        // 변경 없음
        val authHeader = request.getHeader("Authorization") ?: return null
        return if (authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    // handleAdminAuth 와 setSecurityContextHolder 는 더 이상 직접 사용되지 않음.
    // 이들의 로직은 doFilterInternal 내부에서 ScopedValue 컨텍스트 설정 전에 처리됨.
    // private fun handleAdminAuth(accessPayload: AccessPayload) { ... }
    // private fun setSecurityContextHolder(accessPayload: AccessPayload) { ... }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        errorCode: ErrorCode,
    ) {
        // 변경 없음
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

    // RequestCacheData의 필드명 변경: shouldNotFilter -> isPublicForFiltering
    data class RequestCacheData(
        val handlerMethod: HandlerMethod?,
        val isPublicForFiltering: Boolean, // @Public 어노테이션이 있는지 여부
        val isPublicWithTokenCheck: Boolean,
        val requiresAdminAuth: Boolean,
    )
}
