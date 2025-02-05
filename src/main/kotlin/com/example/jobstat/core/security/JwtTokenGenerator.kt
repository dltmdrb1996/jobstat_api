package com.example.jobstat.core.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenGenerator(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.accessTokenExpiration}") private val accessTokenExpiration: Int,
    @Value("\${jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Int,
) {

    private companion object {
        private val HEADER_MAP: Map<String, Any> = mapOf(
            "typ" to "JWT",
            "alg" to "HS256"
        )
    }

    private val key : SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun createAccessToken(payload: AccessPayload): String =
        createToken(payload.id, payload.roles, payload.tokenType, accessTokenExpiration)

    fun createRefreshToken(payload: RefreshPayload): String =
        createToken(payload.id, payload.roles, payload.tokenType, refreshTokenExpiration)

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration.toLong()

    private fun createToken(
        id: Long,
        roles: List<String>,
        tokenType: TokenType,
        expirationInSeconds: Int,
    ): String {
        val now = System.currentTimeMillis()
        val expiration = now + (expirationInSeconds * 1000)

        return Jwts.builder()
            .setHeader(HEADER_MAP)
            .claim("userId", id)
            .claim("tokenType", tokenType.value)
            .claim("roles", roles)
            .setIssuedAt(Date(now))
            .setExpiration(Date(expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
}
