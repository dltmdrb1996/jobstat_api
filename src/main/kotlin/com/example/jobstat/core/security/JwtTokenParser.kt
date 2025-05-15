package com.example.jobstat.core.security

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtTokenParser(
    @Value("\${jwt.secret}") private val secret: String,
) {
    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    private val jwtParser =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()

    @Suppress("UNCHECKED_CAST")
    fun validateToken(token: String): AccessPayload =
        try {
            val claims = jwtParser.parseClaimsJws(token).body

            val id = (claims["userId"] as? Number)?.toLong()
                ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "유효하지 않은 JWT 토큰입니다", "userId claim이 존재하지 않음")
            val roles = (claims["roles"] as ArrayList<String>)
            val tokenType = TokenType.fromValue(claims["tokenType"] as Int)
            AccessPayload(
                id = id,
                roles = roles,
                tokenType = tokenType,
            )
        } catch (ex: Exception) {
            val appException =
                when (ex) {
                    is SignatureException ->
                        AppException.fromErrorCode(
                            ErrorCode.TOKEN_INVALID,
                            message = "JWT 서명이 유효하지 않습니다",
                            detailInfo = "잘못된 JWT 서명",
                        )

                    is MalformedJwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.TOKEN_INVALID,
                            message = "JWT 토큰의 형식이 올바르지 않습니다",
                            detailInfo = "잘못된 JWT 토큰 형식",
                        )

                    is ExpiredJwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.TOKEN_INVALID,
                            message = "JWT 토큰이 만료되었습니다",
                            detailInfo = "만료된 JWT 토큰",
                        )

                    is UnsupportedJwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "지원되지 않는 JWT 토큰입니다",
                            detailInfo = "지원되지 않는 JWT 토큰",
                        )

                    is IllegalArgumentException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰이 비어있습니다",
                            detailInfo = "JWT claims 문자열이 비어있음",
                        )

                    is ClassCastException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰의 데이터 형식이 올바르지 않습니다",
                            detailInfo = "잘못된 JWT claims 데이터 타입",
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
