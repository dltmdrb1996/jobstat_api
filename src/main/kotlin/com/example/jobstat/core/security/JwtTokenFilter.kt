package com.example.jobstat.core.security

import com.example.jobstat.auth.user.entity.RoleData
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.annotation.AdminAuth
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.security.annotation.PublicWithTokenCheck
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.concurrent.ConcurrentHashMap

@Component
class JwtTokenFilter(
    private val jwtTokenParser: JwtTokenParser,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val objectMapper: ObjectMapper,
    private val userService: UserService,
) : OncePerRequestFilter() {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    private val errorResponseCache = ConcurrentHashMap<String, String>()
    private val shouldNotFilterCache = ConcurrentHashMap<String, Boolean>()
    private val handlerMethodCache = ConcurrentHashMap<String, HandlerMethod?>()
    private val publicWithTokenCheckCache = ConcurrentHashMap<String, Boolean>()
    private val adminCheckCache = ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val JWT_TOKEN_MISSING = "인증 토큰이 필요합니다"
        private const val JWT_TOKEN_VALIDATION_ERROR = "인증 토큰 검증에 실패했습니다"
        private const val JWT_ADMIN_AUTH_ERROR = "관리자 권한이 필요합니다"
    }

    init {
        // 알려진 에러 케이스들을 미리 캐시
        cacheErrorResponse(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_MISSING)
        cacheErrorResponse(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_VALIDATION_ERROR)
        cacheErrorResponse(ErrorCode.AUTHENTICATION_FAILURE, JWT_ADMIN_AUTH_ERROR)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        shouldNotFilterCache.computeIfAbsent("${request.method}:${request.requestURI}") { _ ->
            val handlerMethod = getHandlerMethodFromCache(request)
            when {
                handlerMethod == null -> false
                handlerMethod.hasMethodAnnotation(Public::class.java) -> true
                handlerMethod.beanType.isAnnotationPresent(Public::class.java) -> true
                else -> false
            }
        }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        log.info("JWT 필터 처리 시작: ${request.requestURI}, ${shouldNotFilter(request)}")
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response)
            return
        }

        val isPublicWithTokenCheck = isPublicWithTokenCheck(request)

        try {
            getJwtFromRequest(request)?.let { jwt ->
                val accessPayload = jwtTokenParser.validateToken(jwt)
                if (checkAdminAuth(request)) {
                    handleAdminAuth(accessPayload)
                } else {
                    setSecurityContextHolder(accessPayload)
                }
            } ?: run {
                if (!isPublicWithTokenCheck) {
                    throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_MISSING)
                }
            }
        } catch (ex: AppException) {
            sendErrorResponse(response, ex)
            return
        } catch (ex: Exception) {
            sendErrorResponse(
                response,
                AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_VALIDATION_ERROR),
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isPublicWithTokenCheck(request: HttpServletRequest): Boolean =
        publicWithTokenCheckCache.computeIfAbsent("${request.method}:${request.requestURI}") { _ ->
            try {
                when (val handler = requestMappingHandlerMapping.getHandler(request)?.handler) {
                    null -> false
                    is HandlerMethod ->
                        handler.hasMethodAnnotation(PublicWithTokenCheck::class.java) ||
                            handler.beanType.isAnnotationPresent(PublicWithTokenCheck::class.java)
                    else -> false
                }
            } catch (ex: Exception) {
                false
            }
        }

    private fun checkAdminAuth(request: HttpServletRequest): Boolean =
        adminCheckCache.computeIfAbsent("${request.method}:${request.requestURI}") { _ ->
            try {
                when (val handler = requestMappingHandlerMapping.getHandler(request)?.handler) {
                    null -> false
                    is HandlerMethod ->
                        handler.hasMethodAnnotation(AdminAuth::class.java) ||
                            handler.beanType.isAnnotationPresent(AdminAuth::class.java)

                    else -> false
                }
            } catch (ex: Exception) {
                false
            }
        }

    private fun handleAdminAuth(accessPayload: AccessPayload) {
        val isAdmin = accessPayload.roles.contains(RoleData.ADMIN.name)
        if (!isAdmin) throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, JWT_ADMIN_AUTH_ERROR)
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

    private fun getJwtFromRequest(request: HttpServletRequest): String? = request.getHeader("Authorization")?.takeIf { it.startsWith(BEARER_PREFIX) }?.substring(BEARER_PREFIX.length)

    private fun sendErrorResponse(
        response: HttpServletResponse,
        appException: AppException,
    ) {
        response.apply {
            characterEncoding = "UTF-8"
            status = appException.httpStatus.value()
            contentType = "application/json; charset=UTF-8"
            writer.write(getErrorResponse(appException))
        }
    }

    private fun cacheErrorResponse(
        errorCode: ErrorCode,
        message: String,
    ) {
        val cacheKey = createCacheKey(errorCode, message)
        if (!errorResponseCache.containsKey(cacheKey)) {
            val errorResponse =
                mapOf(
                    "code" to errorCode.code,
                    "status" to errorCode.defaultHttpStatus.value(),
                    "message" to message,
                )
            errorResponseCache[cacheKey] = objectMapper.writeValueAsString(errorResponse)
        }
    }

    private fun getHandlerMethodFromCache(request: HttpServletRequest): HandlerMethod? {
        val cacheKey = "${request.method}:${request.requestURI}"
        return handlerMethodCache.computeIfAbsent(cacheKey) { _ ->
            (requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod)
        }
    }

    private fun getErrorResponse(appException: AppException): String {
        val cacheKey = createCacheKey(appException.errorCode, appException.message)
        return errorResponseCache.getOrPut(cacheKey) {
            val errorResponse =
                mapOf(
                    "code" to appException.httpStatus.value(),
                    "status" to appException.httpStatus.name,
                    "message" to appException.message,
                )
            objectMapper.writeValueAsString(errorResponse)
        }
    }

    private fun createCacheKey(
        errorCode: ErrorCode,
        message: String,
    ): String = "${errorCode.name}:$message"
}
