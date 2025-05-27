package com.wildrew.jobstat.core.core_token

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_token.model.AccessPayload
import com.wildrew.jobstat.core.core_token.model.TokenType
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.crypto.SecretKey

class JwtTokenParser(
    private val secret: String,
) {
    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    private val jwtParser: JwtParser =
        Jwts
            .parser()
            .verifyWith(key)
            .build()

    init {
        log.info("JWT Token Parser initialized with secret key length: $secret characters")
    }

    @Suppress("UNCHECKED_CAST")
    fun validateToken(token: String): AccessPayload =
        try {
            val claims = jwtParser.parseSignedClaims(token).payload

            val id =
                (claims["userId"] as? Number)?.toLong()
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
                    is SignatureException, is JwtException ->
                        AppException.fromErrorCode(
                            ErrorCode.TOKEN_INVALID,
                            message = ex.message ?: "JWT 서명 또는 형식이 유효하지 않습니다.",
                            detailInfo = ex.message,
                        )

                    is IllegalArgumentException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰 인수가 유효하지 않습니다.",
                            detailInfo = ex.message,
                        )

                    is ClassCastException ->
                        AppException.fromErrorCode(
                            ErrorCode.AUTHENTICATION_FAILURE,
                            message = "JWT 토큰의 데이터 형식이 올바르지 않습니다",
                            detailInfo = "잘못된 JWT claims 데이터 타입: ${ex.message}",
                        )
                    else ->
                        AppException.fromErrorCode(
                            ErrorCode.INTERNAL_ERROR,
                            message = "JWT 처리 중 예상치 못한 오류가 발생했습니다: ${ex.localizedMessage}",
                            detailInfo = ex.toString(),
                        )
                }
            throw appException
        }
}
