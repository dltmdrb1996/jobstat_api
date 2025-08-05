package com.wildrew.apigateway

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_token.JwtTokenParser
import com.wildrew.jobstat.core.core_token.model.AccessPayload
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.SignatureException

@Component
class AuthenticationFilter(
    private val jwtTokenParser: JwtTokenParser,
    private val objectMapper: ObjectMapper,
) : GlobalFilter, Ordered {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        const val HEADER_USER_ID = "X-User-Id"
        const val HEADER_USER_ROLES = "X-User-Roles"

        private val GATEWAY_SPECIFIC_PUBLIC_PATHS_PATTERNS =
            listOf(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/actuator/**",
            )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path

        if (isGatewaySpecificPublicPath(path)) {
            return chain.filter(exchange)
        }

        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange)
        }

        val token = authHeader.substring(BEARER_PREFIX.length)
        var accessPayload: AccessPayload? = null

        try {
            accessPayload = jwtTokenParser.validateToken(token)
        } catch (e: Exception) {
            val (errorMessage, errorCode, httpStatus) =
                when (e) {
                    is SignatureException, is MalformedJwtException -> Triple("유효하지 않은 토큰입니다.", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)
                    is ExpiredJwtException -> Triple("토큰만료", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)
                    is AppException -> Triple(e.message, e.errorCode, HttpStatus.valueOf(e.errorCode.defaultHttpStatus.value()))
                    else -> {
                        log.error("경로에 대한 토큰 검증 중 예상치 못한 오류가 발생했습니다.: {}", path, e)
                        Triple("토큰 검증 중 내부 서버 오류가 발생했습니다.", ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
                    }
                }
            return onError(exchange, errorMessage, httpStatus, errorCode)
        }

        if (accessPayload != null) {
            val mutatedRequest =
                request
                    .mutate()
                    .header(HEADER_USER_ID, accessPayload.id.toString())
                    .header(HEADER_USER_ROLES, accessPayload.roles.joinToString(","))
                    .build()
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
        } else {
            log.warn("Token was present but validation resulted in null payload for path: {}", path)
            return onError(exchange, "Token validation failed unexpectedly", HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID)
        }
    }

    override fun getOrder(): Int { // *** 추가됨 ***
        return -1
    }

    private fun isGatewaySpecificPublicPath(path: String): Boolean =
        GATEWAY_SPECIFIC_PUBLIC_PATHS_PATTERNS.any { pattern ->
            if (pattern.endsWith("/**")) {
                path.startsWith(pattern.removeSuffix("/**"))
            } else {
                path == pattern
            }
        }

    private fun onError(
        exchange: ServerWebExchange,
        errMessage: String,
        httpStatus: HttpStatus,
        errorCode: ErrorCode,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = httpStatus
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse =
            ApiResponse<Unit>(
                code = errorCode.defaultHttpStatus.value(),
                status = errorCode.defaultHttpStatus,
                message = errMessage,
            )

        val dataBuffer: DataBuffer
        try {
            dataBuffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponse))
        } catch (e: JsonProcessingException) {
            log.error("Error writing JSON error response", e)
            response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            return response.setComplete()
        }
        return response.writeWith(Mono.just(dataBuffer))
    }
}