package com.wildrew.apigateway // 패키지명은 프로젝트에 맞게

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
    private val jwtTokenParser: JwtTokenParser, // core_token 모듈에서 주입
    private val objectMapper: ObjectMapper     // core_serializer 모듈 또는 Spring Boot 기본 ObjectMapper
) : AbstractGatewayFilterFactory<AuthenticationFilter.Config>(Config::class.java) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        const val HEADER_USER_ID = "X-User-Id"
        const val HEADER_USER_ROLES = "X-User-Roles"
        private val GATEWAY_SPECIFIC_PUBLIC_PATHS = listOf(
            "/actuator/health",
            "/actuator/prometheus",
            "/actuator/info"
        )
    }

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val path = request.uri.path

            // Gateway 자체에서 특별히 공개 처리할 경로 (예: 자체 health check)
            if (isGatewaySpecificPublicPath(path)) {
                return@GatewayFilter chain.filter(exchange)
            }

            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                // 토큰이 있는 경우: 검증 시도
                val token = authHeader.substring(BEARER_PREFIX.length)
                var accessPayload: AccessPayload? = null

                try {
                    accessPayload = jwtTokenParser.validateToken(token)
                } catch (e: Exception) {
                    val (errorMessage, errorCode, httpStatus) = when (e) {
                        is SignatureException, is MalformedJwtException -> Triple("Invalid token signature or format", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)
                        is ExpiredJwtException -> Triple("Token has expired", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)
                        is AppException -> Triple(e.message ?: "Token validation failed", e.errorCode, HttpStatus.valueOf(e.errorCode.defaultHttpStatus.value()))
                        else -> {
                            log.error("Unexpected error during token validation for path: {}", path, e)
                            Triple("Internal server error during token validation", ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
                        }
                    }
                    // 토큰 검증 실패 시 에러 응답 (이 부분은 유지)
                    return@GatewayFilter onError(exchange, errorMessage, httpStatus, errorCode)
                }

                if (accessPayload != null) {
                    // 유효한 토큰이면 사용자 정보 헤더 추가
                    val mutatedRequest = request.mutate()
                        .header(HEADER_USER_ID, accessPayload.id.toString())
                        .header(HEADER_USER_ROLES, accessPayload.roles.joinToString(","))
                        .build()
                    return@GatewayFilter chain.filter(exchange.mutate().request(mutatedRequest).build())
                } else {
                    // 토큰이 있었지만 validateToken 결과가 null인 경우 (이론상 드묾, validateToken에서 예외 발생해야 함)
                    log.warn("Token was present but validation resulted in null payload for path: {}", path)
                    return@GatewayFilter onError(exchange, "Token validation failed unexpectedly", HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID)
                }
            } else {
                // 토큰이 없는 경우: 아무런 헤더도 추가하지 않고 그대로 다음 필터로 진행
                // 각 마이크로서비스에서 이 경우를 "비인증 사용자"로 간주하고, @Public 등에 따라 처리
                return@GatewayFilter chain.filter(exchange)
            }
        }
    }

    private fun isGatewaySpecificPublicPath(path: String): Boolean {
        return GATEWAY_SPECIFIC_PUBLIC_PATHS.any { publicPath ->
            if (publicPath.endsWith("/**")) {
                path.startsWith(publicPath.removeSuffix("/**"))
            } else {
                path == publicPath
            }
        }
    }

//    // 현재는 일단 통과시키는데 후에는 앤드포인트 개선 혹은 각 개별 pulbic을 추가
//    private fun isPublicPath(path: String): Boolean {
//        return PUBLIC_PATHS.any { publicPath ->
//            if (publicPath.endsWith("/**")) { // 와일드카드 경로 처리
//                path.startsWith(publicPath.removeSuffix("/**"))
//            } else {
//                path == publicPath
//            }
//        }
//    }

    private fun onError(exchange: ServerWebExchange, errMessage: String, httpStatus: HttpStatus, errorCode: ErrorCode): Mono<Void> {
        val response = exchange.response
        response.statusCode = httpStatus
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse = ApiResponse<Unit>(
            code = errorCode.defaultHttpStatus.value(), // 또는 httpStatus.value()
            status = errorCode.defaultHttpStatus, // 또는 httpStatus
            message = errMessage
        )

        val dataBuffer: DataBuffer
        try {
            dataBuffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponse))
        } catch (e: JsonProcessingException) {
            log.error("Error writing JSON error response", e)
            // JSON 변환 실패 시 기본 응답
            response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
            return response.setComplete()
        }
        return response.writeWith(Mono.just(dataBuffer))
    }

    // 이 필터에 대한 설정 클래스 (필요하다면)
    class Config {
        // 예: 특정 경로에만 필터를 적용하거나, 필터 동작을 변경하는 설정값들을 여기에 추가할 수 있음
        // var applicablePaths: List<String>? = null
    }
}