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
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
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
) : AbstractGatewayFilterFactory<AuthenticationFilter.Config>(Config::class.java) {
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

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val path = request.uri.path

            if (isGatewaySpecificPublicPath(path)) {
                return@GatewayFilter chain.filter(exchange)
            }

            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                val token = authHeader.substring(BEARER_PREFIX.length)
                var accessPayload: AccessPayload? = null

                try {
                    accessPayload = jwtTokenParser.validateToken(token)
                } catch (e: Exception) {
                    val (errorMessage, errorCode, httpStatus) =
                        when (e) {
                            is SignatureException, is MalformedJwtException -> Triple("Invalid token signature or format", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)
                            is ExpiredJwtException -> Triple("Token has expired", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)
                            is AppException -> Triple(e.message, e.errorCode, HttpStatus.valueOf(e.errorCode.defaultHttpStatus.value()))
                            else -> {
                                log.error("Unexpected error during token validation for path: {}", path, e)
                                Triple("Internal server error during token validation", ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
                            }
                        }
                    return@GatewayFilter onError(exchange, errorMessage, httpStatus, errorCode)
                }

                if (accessPayload != null) {
                    val mutatedRequest =
                        request
                            .mutate()
                            .header(HEADER_USER_ID, accessPayload.id.toString())
                            .header(HEADER_USER_ROLES, accessPayload.roles.joinToString(","))
                            .build()
                    return@GatewayFilter chain.filter(exchange.mutate().request(mutatedRequest).build())
                } else {
                    log.warn("Token was present but validation resulted in null payload for path: {}", path)
                    return@GatewayFilter onError(exchange, "Token validation failed unexpectedly", HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID)
                }
            } else {
                return@GatewayFilter chain.filter(exchange)
            }
        }
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

    class Config
}
