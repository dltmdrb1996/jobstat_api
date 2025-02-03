package com.example.jobstat.core.security

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.error.StructuredLogger
import com.example.jobstat.core.security.annotation.Public
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
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
) : OncePerRequestFilter() {
    private val log = StructuredLogger(this::class.java)
    private val errorResponseCache = ConcurrentHashMap<String, String>()
    private val shouldNotFilterCache = ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val JWT_TOKEN_MISSING = "허용되지 않은 접근입니다"
        private const val JWT_TOKEN_VALIDATION_ERROR = "보안 검증 중 오류가 발생했습니다"
    }

    init {
        // 알려진 에러 케이스들을 미리 캐시
        cacheErrorResponse(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_MISSING)
        cacheErrorResponse(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_VALIDATION_ERROR)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        shouldNotFilterCache.computeIfAbsent("${request.method}:${request.requestURI}") { _ ->
            try {
                when (val handler = requestMappingHandlerMapping.getHandler(request)?.handler) {
                    null -> false
                    is HandlerMethod -> {
                        handler.hasMethodAnnotation(Public::class.java) ||
                            handler.beanType.isAnnotationPresent(Public::class.java)
                    }
                    else -> false
                }
            } catch (ex: Exception) {
                false
            }
        }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        log.info("doFilterInternal ${request.requestURI}")
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            getJwtFromRequest(request)?.let { jwt ->
                val accessPayload = jwtTokenParser.validateToken(jwt)
                val authentication = UsernamePasswordAuthenticationToken(accessPayload.id, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            } ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_MISSING)
        } catch (ex: AppException) {
            log.error("JWT 토큰 검증 실패", ex)
            sendErrorResponse(response, ex)
            return
        } catch (ex: Exception) {
            log.error("JWT 토큰 검증 중 오류 발생", ex)
            sendErrorResponse(
                response,
                AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, JWT_TOKEN_VALIDATION_ERROR),
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
