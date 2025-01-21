package com.example.jobstat.core.security

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.error.StructuredLogger
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtTokenParser(
    @Value("\${jwt.secret}") private val secret: String,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())
    private val log = StructuredLogger(this::class.java)

    fun validateToken(token: String): AccessPayload {
        try {

            log.info("JWT 토큰 검증 시작 ${token}")
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            val userId = if(claims["userId"] is Int) {
                (claims["userId"] as Int).toLong()
            } else {
                claims["userId"] as Long
            }

            val tokenType = TokenType.fromValue(claims["tokenType"] as Int)
            require(tokenType in listOf(TokenType.ACCESS_TOKEN, TokenType.REFRESH_TOKEN)) { "올바른 토큰타입이 아닙니다." }
            return AccessPayload(userId, tokenType)
        }  catch (ex: Exception) {
            val appException =
                when (ex) {
                    is SignatureException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 서명이 유효하지 않습니다",
                            detailInfo = "Invalid JWT signature",
                        )
                    is MalformedJwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰의 형식이 올바르지 않습니다",
                            detailInfo = "Malformed JWT token",
                        )
                    is ExpiredJwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰이 만료되었습니다",
                            detailInfo = "Expired JWT token",
                        )
                    is UnsupportedJwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "지원되지 않는 JWT 토큰입니다",
                            detailInfo = "Unsupported JWT token",
                        )
                    is IllegalArgumentException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰이 비어있습니다",
                            detailInfo = "JWT claims string is empty",
                        )
                    is JwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = ex.message ?: "토큰 검증에 실패했습니다",
                            detailInfo = ex.message,
                        )
                    else ->
                        AppException.fromErrorCode(
                            ErrorCode.INTERNAL_ERROR,
                            message = "JWT 처리 중 예상치 못한 오류가 발생했습니다",
                            detailInfo = ex.message,
                        )
                }
            throw appException
        }
    }
}
